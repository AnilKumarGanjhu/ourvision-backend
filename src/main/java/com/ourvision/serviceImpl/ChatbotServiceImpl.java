package com.ourvision.serviceImpl;

import java.text.Normalizer;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ourvision.dto.ChatbotResponse;
import com.ourvision.dto.ChatbotTurn;
import com.ourvision.entity.ChatbotInformation;
import com.ourvision.repository.ChatbotInformationRepository;
import com.ourvision.service.ChatbotService;
import com.ourvision.service.GeminiService;
import com.ourvision.service.GeminiUnavailableException;
import com.ourvision.utill.ChatbotQuestionMatcher;

/**
 * Answer flow:
 *
 * 1. Search the approved knowledge base (chatbot_information) - DATABASE.
 * 2. Built-in small talk (greetings, jokes, date/time) for short messages.
 * 3. Otherwise ask Gemini from the backend - GEMINI.
 * 4. If Gemini is unavailable, return a friendly message - UNAVAILABLE / FALLBACK.
 *
 * Gemini answers are never saved as verified knowledge.
 */
@Service
public class ChatbotServiceImpl implements ChatbotService {

	private static final Logger log = LoggerFactory.getLogger(ChatbotServiceImpl.class);

	private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

	private static final int SMALL_TALK_MAX_WORDS = 8;
	private static final int SMALL_TALK_MAX_EXTRA_WORDS = 1;

	/** Words used by the built-in greetings / casual chat / thanks / goodbye phrases. */
	private static final Set<String> SMALL_TALK_WORDS = new HashSet<>(Arrays.asList("hello", "hi", "hey", "namaste",
			"ram", "good", "morning", "afternoon", "evening", "night", "gm", "gn", "bye", "goodbye", "see", "you",
			"talk", "to", "later", "how", "are", "r", "u", "kaise", "ho", "hain", "kya", "haal", "hai", "kar", "rahe",
			"what", "doing", "thank", "thanks", "shukriya", "dhanyawad", "main", "sone", "ja", "raha", "rahi", "hoon",
			"ab", "chalta", "chalti", "bhai", "bhaiya", "bro", "brother", "dost", "yaar", "friend", "didi", "sister",
			"sis", "boss", "aap", "am", "i", "a", "so", "much", "very", "lot", "ji"));
	private static final int JOKE_MAX_WORDS = 8;
	private static final int DATE_TIME_MAX_WORDS = 7;

	private final ChatbotInformationRepository repository;
	private final GeminiService geminiService;
	private final double matchThreshold;

	public ChatbotServiceImpl(ChatbotInformationRepository repository, GeminiService geminiService,
			@Value("${chatbot.match-threshold:0.65}") double matchThreshold) {
		this.repository = repository;
		this.geminiService = geminiService;
		this.matchThreshold = matchThreshold;
	}

	@Override
	public ChatbotResponse getAnswer(String question, List<ChatbotTurn> history) {

		if (question == null || question.trim().isEmpty()) {
			return new ChatbotResponse("Please apna question type karein. 😊", "FRIENDLY");
		}

		String trimmedQuestion = question.trim();

		// 1. Approved database answers come first. Gemini is NOT called when one matches.
		try {
			ChatbotInformation match = findBestDatabaseMatch(trimmedQuestion);

			if (match != null) {
				return new ChatbotResponse(addFriendlyPrefix(trimmedQuestion, match.getAnswer()), "DATABASE");
			}
		} catch (RuntimeException e) {
			// Never fail the chat because of the lookup; continue with the next steps.
			log.error("Chatbot database search failed: {}", e.getClass().getSimpleName());
		}

		// 2. Built-in small talk. Only for short messages, so that a real question
		// such as "Hi, what is photosynthesis?" still reaches Gemini.
		String normalizedQuestion = normalize(trimmedQuestion);
		int wordCount = normalizedQuestion.isEmpty() ? 0 : normalizedQuestion.split(" ").length;

		if (wordCount <= SMALL_TALK_MAX_WORDS && isOnlySmallTalk(normalizedQuestion)) {
			ChatbotResponse friendlyResponse = getFriendlyResponse(trimmedQuestion);
			if (friendlyResponse != null) {
				return friendlyResponse;
			}
		}

		if (wordCount <= JOKE_MAX_WORDS && isJokeRequest(normalizedQuestion)) {
			return new ChatbotResponse(getRandomJoke(normalizedQuestion), "JOKE");
		}

		if (wordCount <= DATE_TIME_MAX_WORDS && !containsAny(normalizedQuestion, "did", "was", "were", "when")) {
			String dateTimeAnswer = getDateTimeAnswer(normalizedQuestion);
			if (dateTimeAnswer != null) {
				return new ChatbotResponse(dateTimeAnswer, "DATETIME");
			}
		}

		// 3. Gemini fallback (called from the backend only).
		return askGemini(trimmedQuestion, history);
	}

	/**
	 * True when the message is (almost) only greeting / chit-chat words, so that
	 * "Hi, what is photosynthesis?" is sent on to Gemini instead of getting a
	 * canned greeting.
	 */
	private boolean isOnlySmallTalk(String normalizedQuestion) {

		int extraWords = 0;

		for (String word : normalizedQuestion.split(" ")) {
			if (!word.isEmpty() && !SMALL_TALK_WORDS.contains(word)) {
				extraWords++;
			}
		}

		return extraWords <= SMALL_TALK_MAX_EXTRA_WORDS;
	}

	// ---------------------------------------------------------
	// DATABASE SEARCH
	// ---------------------------------------------------------

	private ChatbotInformation findBestDatabaseMatch(String question) {

		List<ChatbotInformation> approvedInformation = repository.findByStatusIgnoreCase("APPROVED");

		if (approvedInformation == null || approvedInformation.isEmpty()) {
			return null;
		}

		// Stable order so ties are resolved the same way every time.
		List<ChatbotInformation> candidates = new ArrayList<>(approvedInformation);
		candidates.sort(Comparator.comparing(ChatbotInformation::getId, Comparator.nullsLast(Comparator.naturalOrder())));

		ChatbotInformation best = null;
		double bestScore = 0.0;

		for (ChatbotInformation information : candidates) {

			if (information == null || information.getQuestion() == null || information.getAnswer() == null
					|| information.getAnswer().trim().isEmpty()) {
				continue;
			}

			double score = ChatbotQuestionMatcher.score(question, information.getQuestion());

			if (score > bestScore) {
				bestScore = score;
				best = information;
			}
		}

		return bestScore >= matchThreshold ? best : null;
	}

	// ---------------------------------------------------------
	// GEMINI FALLBACK
	// ---------------------------------------------------------

	private ChatbotResponse askGemini(String question, List<ChatbotTurn> history) {

		if (!geminiService.isEnabled()) {
			return new ChatbotResponse(getFallbackMessage(question), "FALLBACK");
		}

		try {
			String answer = geminiService.generateAnswer(question, history);
			return new ChatbotResponse(answer, "GEMINI");

		} catch (GeminiUnavailableException e) {

			if (e.getReason() == GeminiUnavailableException.Reason.ACCESS_DENIED) {
				log.error("Gemini rejected the request. Check GEMINI_API_KEY and API access.");
			}

			return new ChatbotResponse(getGeminiUnavailableMessage(e.getReason()), "UNAVAILABLE");

		} catch (RuntimeException e) {
			log.error("Unexpected Gemini error: {}", e.getClass().getSimpleName());
			return new ChatbotResponse(getGeminiUnavailableMessage(null), "UNAVAILABLE");
		}
	}

	private String getGeminiUnavailableMessage(GeminiUnavailableException.Reason reason) {

		if (reason == GeminiUnavailableException.Reason.QUOTA_EXCEEDED) {
			return "Abhi AI service par bahut zyada requests hain. 🙏 Kripya thodi der baad dobara try karein.";
		}

		if (reason == GeminiUnavailableException.Reason.TIMEOUT) {
			return "AI service ko jawab dene mein zyada time lag raha hai. ⏳ Kripya dobara try karein.";
		}

		if (reason == GeminiUnavailableException.Reason.BLOCKED) {
			return "Main is question ka jawab nahi de sakta. 😊 Kripya apna question alag tarike se poochein.";
		}

		return "Abhi main is question ka answer nahi de pa raha hoon. 😊 "
				+ "Kripya thodi der baad try karein, ya admin se sampark karein.";
	}

	// ---------------------------------------------------------
	// FRIENDLY RESPONSES
	// ---------------------------------------------------------

	private ChatbotResponse getFriendlyResponse(String question) {

		String text = normalize(question);

		ZonedDateTime now = ZonedDateTime.now(INDIA_ZONE);
		int hour = now.getHour();

		String timeGreeting = getTimeBasedGreeting(hour);

		// Goodbye must be checked separately from first greetings.
		if (isGoodbyeRequest(text)) {
			String goodbyeMessage;

			if (containsPhrase(text, "good night") || containsPhrase(text, "gn")
					|| containsPhrase(text, "main sone ja raha hoon")
					|| containsPhrase(text, "main sone ja rahi hoon")) {

				goodbyeMessage = "Good night! 😊 Apna khayal rakhna. Phir baat karenge!";

			} else {
				goodbyeMessage = "Bye! 😊 Apna khayal rakhna. Phir baat karenge!";
			}

			return new ChatbotResponse(goodbyeMessage, "FRIENDLY");
		}

		// First greeting: after 9 PM, use Hello instead of Good Night.
		if (isGreetingRequest(text)) {
			String greetingMessage;

			if ("Hello".equals(timeGreeting)) {
				greetingMessage = "Hello! 😊 ";
			} else {
				greetingMessage = timeGreeting + "! 😊 ";
			}

			return new ChatbotResponse(
					greetingMessage + getFriendlyPrefix(question) + "kaise ho? Batao, aaj kya help chahiye?",
					"FRIENDLY");
		}

		// Casual conversation
		if (containsAny(text, "how are you", "how r you", "kaise ho", "kaise hain", "kya haal hai", "kya kar rahe ho",
				"what are you doing")) {

			return new ChatbotResponse("Main badhiya hoon! 😊 Aap batao, main aapki kya help kar sakta hoon?",
					"FRIENDLY");
		}

		// Thank you
		if (containsAny(text, "thank you", "thanks", "thank u", "shukriya", "dhanyawad")) {

			return new ChatbotResponse("You're welcome! 😊 Aapki help karke khushi hui.", "FRIENDLY");
		}

		return null;
	}

	/**
	 * Morning, afternoon and evening greetings are time-based. After 9 PM, the
	 * first greeting is Hello, not Good Night.
	 */
	private String getTimeBasedGreeting(int hour) {

		if (hour >= 5 && hour < 12) {
			return "Good morning";
		}

		if (hour >= 12 && hour < 17) {
			return "Good afternoon";
		}

		if (hour >= 17 && hour < 21) {
			return "Good evening";
		}

		return "Hello";
	}

	private boolean isGreetingRequest(String text) {
		return containsAny(text, "hello", "hi", "hey", "namaste", "ram ram", "good morning", "good afternoon",
				"good evening", "good night", "gm", "gn");
	}

	private boolean isGoodbyeRequest(String text) {
		return containsAny(text, "bye", "goodbye", "see you", "talk to you later", "good night", "gn",
				"main sone ja raha hoon", "main sone ja rahi hoon", "ab main chalta hoon", "ab main chalti hoon");
	}

	// ---------------------------------------------------------
	// FRIENDLY PREFIX
	// ---------------------------------------------------------

	private String getFriendlyPrefix(String question) {

		String text = normalize(question);

		if (containsAny(text, "bhai", "bhaiya", "bro", "brother")) {
			return "Bhai 😎, ";
		}

		if (containsAny(text, "dost", "yaar", "friend")) {
			return "Dost 😊, ";
		}

		if (containsAny(text, "didi", "sister", "sis")) {
			return "Bilkul 😊, ";
		}

		if (containsAny(text, "boss")) {
			return "Ji boss 😊, ";
		}

		return "";
	}

	private String addFriendlyPrefix(String question, String answer) {

		if (answer == null || answer.trim().isEmpty()) {
			return getFallbackMessage(question);
		}

		String prefix = getFriendlyPrefix(question);

		if (prefix.isEmpty()) {
			return answer;
		}

		return prefix + answer;
	}

	// ---------------------------------------------------------
	// JOKES
	// ---------------------------------------------------------

	private boolean isJokeRequest(String text) {
		return containsAny(text, "joke", "jokes", "funny", "make me laugh", "hasao", "chutkula", "chutkule", "comedy");
	}

	private String getRandomJoke(String question) {

		List<String> jokes = new ArrayList<>();

		if (containsAny(question, "programming", "programmer", "java", "coding")) {
			jokes.add(
					"Programmer ki life mein 2 problems hoti hain: code nahi chalta, aur code chal jaye toh pata nahi kyun chalta hai! 😂");
			jokes.add("Java developer coffee kyun peeta hai? Kyunki uski life mein bahut saare exceptions hain! ☕");
			jokes.add("Bug developer ka purana dost hai, har release mein milne aa jata hai! 😄");
		} else if (containsAny(question, "office", "boss", "manager", "meeting")) {
			jokes.add("Office mein sabse fast kaun bhaagta hai? Friday ko office se nikalne wala employee! 😂");
			jokes.add("Meeting ka sabse important result kya hota hai? Agli meeting schedule hona! 😄");
		} else if (containsAny(question, "student", "school", "exam", "study")) {
			jokes.add("Exam mein student ka favourite chapter kaunsa hota hai? Jo syllabus se bahar ho! 😂");
			jokes.add("Student: Sir, main absent kyun tha? Sir: Ye toh tumhe pata hona chahiye! 😄");
		} else {
			jokes.add(
					"Teacher: Batao, sabse zyada nasha kis cheez mein hota hai? Student: Books mein, kholte hi neend aa jaati hai! 😂");
			jokes.add("Ek computer ne doosre computer se kaha: Tum bahut slow ho, thoda update ho jao! 😄");
			jokes.add("Mummy: Phone rakh do. Beta: Bas 5 minute. Mummy: Tumhare 5 minute ka alag calendar hai kya? 😂");
			jokes.add(
					"Dost: Tum itne khush kyun ho? Doosra: Aaj alarm se pehle meri neend khul gayi, alarm ab mujhse jealous hai! 😄");
		}

		Collections.shuffle(jokes);
		return jokes.get(0);
	}

	// ---------------------------------------------------------
	// DATE AND TIME
	// ---------------------------------------------------------

	private String getDateTimeAnswer(String question) {

		ZonedDateTime now = ZonedDateTime.now(INDIA_ZONE);

		if (containsAny(question, "current time", "what time", "time now", "time is it", "abhi kitne baje",
				"kitna time hua", "samay kya hai", "time batao")) {

			String currentTime = now.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));

			return "Abhi India mein " + currentTime + " baj rahe hain. 🕒";
		}

		if (containsAny(question, "today's date", "current date", "what date", "date today", "aaj ki date",
				"aaj ka date", "tarikh kya hai", "aaj kya tarikh hai")) {

			String currentDate = now.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH));

			return "Aaj ki date " + currentDate + " hai. 📅";
		}

		if (containsAny(question, "what day", "which day", "today is", "aaj ka din", "aaj kaun sa din", "aaj konsa din",
				"day today")) {

			String currentDay = now.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH));

			return "Aaj " + currentDay + " hai. 😊";
		}

		if (containsAny(question, "current month", "which month", "what month", "kaunsa mahina", "kaun sa mahina")) {

			String currentMonth = now.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));

			return "Abhi " + currentMonth + " chal raha hai. 📅";
		}

		if (containsAny(question, "current year", "which year", "what year", "kaunsa saal", "kaun sa saal")) {

			return "Abhi saal " + now.getYear() + " hai. 📅";
		}

		return null;
	}

	// ---------------------------------------------------------
	// FALLBACK
	// ---------------------------------------------------------

	private String getFallbackMessage(String question) {

		String prefix = getFriendlyPrefix(question);

		return prefix + "Is question ka exact answer abhi mere knowledge base mein nahi hai. 😊 "
				+ "Aap apna question thoda alag tarike se pooch sakte hain, "
				+ "ya admin se is question ka answer add karne ko keh sakte hain.";
	}

	// ---------------------------------------------------------
	// TEXT MATCHING HELPERS
	// ---------------------------------------------------------

	private boolean containsAny(String text, String... phrases) {

		if (text == null || phrases == null) {
			return false;
		}

		for (String phrase : phrases) {
			if (containsPhrase(text, phrase)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Uses word boundaries for short greetings such as "hi", "gm" and "gn",
	 * avoiding accidental matches inside unrelated words.
	 */
	private boolean containsPhrase(String text, String phrase) {

		if (text == null || phrase == null) {
			return false;
		}

		String normalizedText = normalize(text);
		String normalizedPhrase = normalize(phrase);

		if (normalizedText.isEmpty() || normalizedPhrase.isEmpty()) {
			return false;
		}

		String regex = "(^|\\s)" + java.util.regex.Pattern.quote(normalizedPhrase) + "($|\\s)";

		return java.util.regex.Pattern.compile(regex).matcher(normalizedText).find();
	}

	private String normalize(String text) {

		if (text == null) {
			return "";
		}

		return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
	}
}
