package com.ourvision.serviceImpl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ourvision.dto.ChatbotTurn;
import com.ourvision.service.GeminiService;
import com.ourvision.service.GeminiUnavailableException;
import com.ourvision.service.GeminiUnavailableException.Reason;

/**
 * Calls the Gemini generateContent API from the backend.
 *
 * Ported from the standalone ChatBot.zip prototype (model fallback chain,
 * multi-turn history, system prompt, temperature and timeout), but the API key
 * now lives only in server configuration and is sent in a request header, so it
 * never appears in URLs, logs, API responses or frontend code.
 */
@Service
public class GeminiServiceImpl implements GeminiService {

	private static final Logger log = LoggerFactory.getLogger(GeminiServiceImpl.class);

	private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
	private static final Pattern SAFE_MODEL_NAME = Pattern.compile("^[A-Za-z0-9._-]+$");
	private static final int MAX_TURN_CHARS = 1000;

	private static final long COOLDOWN_BUSY_MS = 30_000L;
	private static final long COOLDOWN_QUOTA_MS = 60_000L;
	private static final long COOLDOWN_MISSING_MS = 10 * 60_000L;

	private static final String DEFAULT_SYSTEM_PROMPT = "You are Our Vision Assistant, the helper chatbot of the Our Vision "
			+ "community web application. Answer the user's questions helpfully, accurately and concisely. "
			+ "Reply in the same language and style the user writes in (English, Hindi or Hinglish). "
			+ "You do NOT have access to Our Vision's private data such as members, finances, deposits, elections, "
			+ "attendance, meetings or events. If asked for such details, or for any Our Vision specific fact you "
			+ "cannot know, say that you do not have that information and suggest contacting an admin. "
			+ "Never invent facts. Use simple markdown (bold text, short bullet lists) only when it helps.";

	private final ObjectMapper mapper = new ObjectMapper();
	private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

	/** model -> epoch millis until which the model is skipped (busy / quota / missing). */
	private final Map<String, Long> cooldownUntil = new ConcurrentHashMap<>();

	private final boolean enabled;
	private final String apiKey;
	private final String baseUrl;
	private final List<String> models;
	private final double temperature;
	private final int maxOutputTokens;
	private final int requestTimeoutSeconds;
	private final int totalTimeoutSeconds;
	private final int maxHistoryTurns;
	private final String systemPrompt;

	public GeminiServiceImpl(@Value("${chatbot.gemini.enabled:true}") boolean enabled,
			@Value("${chatbot.gemini.api-key:}") String apiKey,
			@Value("${chatbot.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
			@Value("${chatbot.gemini.models:gemini-3.8-flash,gemini-3.5-flash,gemini-3.5-flash-lite}") String models,
			@Value("${chatbot.gemini.temperature:0.7}") double temperature,
			@Value("${chatbot.gemini.max-output-tokens:2048}") int maxOutputTokens,
			@Value("${chatbot.gemini.request-timeout-seconds:15}") int requestTimeoutSeconds,
			@Value("${chatbot.gemini.total-timeout-seconds:25}") int totalTimeoutSeconds,
			@Value("${chatbot.history-max-turns:6}") int maxHistoryTurns,
			@Value("${chatbot.gemini.system-prompt:}") String systemPrompt) {

		this.enabled = enabled;
		this.apiKey = apiKey == null ? "" : apiKey.trim();
		this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		this.models = parseModels(models);
		this.temperature = temperature;
		this.maxOutputTokens = maxOutputTokens;
		this.requestTimeoutSeconds = Math.max(1, requestTimeoutSeconds);
		this.totalTimeoutSeconds = Math.max(this.requestTimeoutSeconds, totalTimeoutSeconds);
		this.maxHistoryTurns = Math.max(0, maxHistoryTurns);
		this.systemPrompt = (systemPrompt == null || systemPrompt.isBlank()) ? DEFAULT_SYSTEM_PROMPT
				: systemPrompt.trim();

		if (isEnabled()) {
			log.info("Gemini fallback enabled. Models: {}", this.models);
		} else {
			log.warn("Gemini fallback is NOT active (GEMINI_API_KEY missing, disabled, or no valid model configured). "
					+ "The chatbot will answer from the database only.");
		}
	}

	@Override
	public boolean isEnabled() {
		return enabled && !apiKey.isEmpty() && !models.isEmpty();
	}

	@Override
	public String generateAnswer(String question, List<ChatbotTurn> history) throws GeminiUnavailableException {

		if (!isEnabled()) {
			throw new GeminiUnavailableException(Reason.NOT_CONFIGURED, "Gemini is not configured");
		}

		String body;
		try {
			body = mapper.writeValueAsString(buildPayload(question, history));
		} catch (JsonProcessingException e) {
			throw new GeminiUnavailableException(Reason.UPSTREAM_ERROR, "Could not build request");
		}

		long deadline = System.currentTimeMillis() + totalTimeoutSeconds * 1000L;
		GeminiUnavailableException failure = null;

		for (String model : orderedModels()) {

			long remainingMs = deadline - System.currentTimeMillis();
			if (remainingMs <= 500) {
				if (failure == null) {
					failure = new GeminiUnavailableException(Reason.TIMEOUT, "Total timeout reached");
				}
				break;
			}

			Duration timeout = Duration.ofMillis(Math.min(requestTimeoutSeconds * 1000L, remainingMs));

			try {
				String text = callModel(model, body, timeout);
				cooldownUntil.remove(model);
				return text;

			} catch (GeminiUnavailableException e) {

				// Problems that every model would share: stop immediately.
				if (e.getReason() == Reason.ACCESS_DENIED || e.getReason() == Reason.BLOCKED) {
					throw e;
				}

				// Remember the most useful failure to report (quota beats the rest).
				if (failure == null || e.getReason() == Reason.QUOTA_EXCEEDED) {
					failure = e;
				}
			}
		}

		throw failure != null ? failure : new GeminiUnavailableException(Reason.UPSTREAM_ERROR, "No model answered");
	}

	// ---------------------------------------------------------
	// REQUEST
	// ---------------------------------------------------------

	private ObjectNode buildPayload(String question, List<ChatbotTurn> history) {

		ObjectNode payload = mapper.createObjectNode();

		ArrayNode contents = payload.putArray("contents");
		for (ObjectNode content : buildContents(question, history)) {
			contents.add(content);
		}

		String dateLine = "Current date and time in India (IST): "
				+ ZonedDateTime.now(INDIA_ZONE).format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy, hh:mm a", Locale.ENGLISH));

		ObjectNode systemInstruction = payload.putObject("systemInstruction");
		systemInstruction.putArray("parts").addObject().put("text", systemPrompt + "\n" + dateLine);

		ObjectNode generationConfig = payload.putObject("generationConfig");
		generationConfig.put("temperature", temperature);
		generationConfig.put("maxOutputTokens", maxOutputTokens);

		return payload;
	}

	/**
	 * Builds a valid alternating user/model conversation that always starts and
	 * ends with a user turn.
	 */
	private List<ObjectNode> buildContents(String question, List<ChatbotTurn> history) {

		List<String[]> turns = new ArrayList<>(); // [role, text]

		if (history != null && maxHistoryTurns > 0) {
			int from = Math.max(0, history.size() - maxHistoryTurns);

			for (ChatbotTurn turn : history.subList(from, history.size())) {
				if (turn == null || turn.getText() == null || turn.getText().isBlank() || turn.getRole() == null) {
					continue;
				}

				String role = mapRole(turn.getRole());
				if (role == null) {
					continue;
				}

				addTurn(turns, role, truncate(turn.getText().trim()));
			}
		}

		// A conversation must start with a user turn.
		while (!turns.isEmpty() && "model".equals(turns.get(0)[0])) {
			turns.remove(0);
		}

		addTurn(turns, "user", truncate(question.trim()));

		List<ObjectNode> contents = new ArrayList<>();
		for (String[] turn : turns) {
			ObjectNode content = mapper.createObjectNode();
			content.put("role", turn[0]);
			content.putArray("parts").addObject().put("text", turn[1]);
			contents.add(content);
		}
		return contents;
	}

	private void addTurn(List<String[]> turns, String role, String text) {
		if (!turns.isEmpty() && turns.get(turns.size() - 1)[0].equals(role)) {
			String[] last = turns.get(turns.size() - 1);
			last[1] = last[1] + "\n\n" + text; // merge consecutive same-role turns
		} else {
			turns.add(new String[] { role, text });
		}
	}

	private String mapRole(String role) {
		String r = role.trim().toLowerCase(Locale.ROOT);
		if (r.equals("user")) {
			return "user";
		}
		if (r.equals("bot") || r.equals("model") || r.equals("assistant")) {
			return "model";
		}
		return null;
	}

	private String truncate(String text) {
		return text.length() > MAX_TURN_CHARS ? text.substring(0, MAX_TURN_CHARS) : text;
	}

	// ---------------------------------------------------------
	// HTTP CALL
	// ---------------------------------------------------------

	private String callModel(String model, String body, Duration timeout) throws GeminiUnavailableException {

		HttpRequest request = HttpRequest
				.newBuilder(URI.create(baseUrl + "/v1beta/models/" + model + ":generateContent")).timeout(timeout)
				.header("Content-Type", "application/json").header("x-goog-api-key", apiKey)
				.POST(BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();

		HttpResponse<String> response;

		try {
			response = httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));

		} catch (HttpTimeoutException e) {
			cooldown(model, COOLDOWN_BUSY_MS);
			log.warn("Gemini model {} timed out after {}s", model, timeout.toSeconds());
			throw new GeminiUnavailableException(Reason.TIMEOUT, "Timeout");

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new GeminiUnavailableException(Reason.UPSTREAM_ERROR, "Interrupted");

		} catch (IOException e) {
			cooldown(model, COOLDOWN_BUSY_MS);
			// Log only the exception type: messages can contain URLs.
			log.warn("Gemini model {} request failed: {}", model, e.getClass().getSimpleName());
			throw new GeminiUnavailableException(Reason.UPSTREAM_ERROR, "I/O error");
		}

		int status = response.statusCode();

		if (status == 200) {
			return parseAnswer(model, response.body());
		}

		String errorSummary = summarizeError(response.body());
		log.warn("Gemini model {} returned HTTP {} {}", model, status, errorSummary);

		switch (status) {
		case 429:
			cooldown(model, COOLDOWN_QUOTA_MS);
			throw new GeminiUnavailableException(Reason.QUOTA_EXCEEDED, "HTTP 429");
		case 401:
		case 403:
			throw new GeminiUnavailableException(Reason.ACCESS_DENIED, "HTTP " + status);
		case 400:
			// Gemini reports an invalid API key as HTTP 400.
			if (response.body() != null && response.body().toLowerCase(Locale.ROOT).contains("api key")) {
				throw new GeminiUnavailableException(Reason.ACCESS_DENIED, "Invalid API key");
			}
			cooldown(model, COOLDOWN_MISSING_MS);
			throw new GeminiUnavailableException(Reason.UPSTREAM_ERROR, "HTTP 400");
		case 404:
			cooldown(model, COOLDOWN_MISSING_MS);
			throw new GeminiUnavailableException(Reason.UPSTREAM_ERROR, "Model not found");
		case 408:
		case 504:
			cooldown(model, COOLDOWN_BUSY_MS);
			throw new GeminiUnavailableException(Reason.TIMEOUT, "HTTP " + status);
		default:
			cooldown(model, COOLDOWN_BUSY_MS);
			throw new GeminiUnavailableException(Reason.UPSTREAM_ERROR, "HTTP " + status);
		}
	}

	private String parseAnswer(String model, String responseBody) throws GeminiUnavailableException {

		JsonNode root;
		try {
			root = mapper.readTree(responseBody);
		} catch (IOException e) {
			throw new GeminiUnavailableException(Reason.UPSTREAM_ERROR, "Unreadable response");
		}

		if (root == null) {
			throw new GeminiUnavailableException(Reason.EMPTY_RESPONSE, "Empty body");
		}

		if (!root.path("promptFeedback").path("blockReason").asText("").isEmpty()) {
			throw new GeminiUnavailableException(Reason.BLOCKED, "Prompt blocked");
		}

		JsonNode candidate = root.path("candidates").path(0);

		StringBuilder text = new StringBuilder();
		for (JsonNode part : candidate.path("content").path("parts")) {
			// Skip "thinking" parts of reasoning models.
			if (part.path("thought").asBoolean(false)) {
				continue;
			}
			text.append(part.path("text").asText(""));
		}

		String answer = text.toString().trim();

		if (answer.isEmpty()) {
			String finishReason = candidate.path("finishReason").asText("");
			if (finishReason.equals("SAFETY") || finishReason.equals("PROHIBITED_CONTENT")
					|| finishReason.equals("BLOCKLIST") || finishReason.equals("SPII")) {
				throw new GeminiUnavailableException(Reason.BLOCKED, "Response blocked: " + finishReason);
			}
			cooldown(model, COOLDOWN_BUSY_MS);
			throw new GeminiUnavailableException(Reason.EMPTY_RESPONSE, "No text returned");
		}

		return answer;
	}

	/** Extracts Google's error status/message (never contains our key), truncated. */
	private String summarizeError(String body) {
		if (body == null || body.isBlank()) {
			return "";
		}
		try {
			JsonNode error = mapper.readTree(body).path("error");
			String summary = error.path("status").asText("") + " " + error.path("message").asText("");
			summary = summary.trim().replace('\n', ' ');
			return summary.length() > 200 ? summary.substring(0, 200) : summary;
		} catch (IOException e) {
			return "";
		}
	}

	// ---------------------------------------------------------
	// MODEL ORDERING / HEALTH
	// ---------------------------------------------------------

	private void cooldown(String model, long millis) {
		cooldownUntil.put(model, System.currentTimeMillis() + millis);
	}

	/** Healthy models first (in configured order), cooled-down ones last. */
	private List<String> orderedModels() {

		long now = System.currentTimeMillis();
		List<String> healthy = new ArrayList<>();
		List<String> coolingDown = new ArrayList<>();

		for (String model : models) {
			Long until = cooldownUntil.get(model);
			if (until != null && until > now) {
				coolingDown.add(model);
			} else {
				healthy.add(model);
			}
		}

		healthy.addAll(coolingDown);
		return healthy;
	}

	private static List<String> parseModels(String csv) {
		List<String> result = new ArrayList<>();
		if (csv == null) {
			return result;
		}
		for (String name : csv.split(",")) {
			String model = name.trim();
			if (!model.isEmpty() && SAFE_MODEL_NAME.matcher(model).matches() && !result.contains(model)) {
				result.add(model);
			}
		}
		return result;
	}
}
