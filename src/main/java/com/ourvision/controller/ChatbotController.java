
package com.ourvision.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ourvision.dto.ChatbotRequest;
import com.ourvision.dto.ChatbotResponse;
import com.ourvision.service.ChatbotService;
import com.ourvision.utill.ChatbotRateLimiter;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

	private static final Logger log = LoggerFactory.getLogger(ChatbotController.class);

	private static final int MAX_MESSAGE_LENGTH = 1000;

	private final ChatbotService chatbotService;
	private final ChatbotRateLimiter rateLimiter;
	private final boolean trustForwardedHeader;

	public ChatbotController(ChatbotService chatbotService, ChatbotRateLimiter rateLimiter,
			@Value("${chatbot.rate-limit.trust-forwarded-header:false}") boolean trustForwardedHeader) {
		this.chatbotService = chatbotService;
		this.rateLimiter = rateLimiter;
		this.trustForwardedHeader = trustForwardedHeader;
	}

	@PostMapping("/message")
	public ResponseEntity<ChatbotResponse> sendMessage(@RequestBody ChatbotRequest request,
			HttpServletRequest httpRequest) {

		if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ChatbotResponse("Please enter a question.", "FALLBACK"));
		}

		if (request.getMessage().length() > MAX_MESSAGE_LENGTH) {

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ChatbotResponse(
							"Question bahut lamba hai. Kripya " + MAX_MESSAGE_LENGTH + " characters ke andar likhein.",
							"FALLBACK"));
		}

		if (!rateLimiter.tryAcquire(resolveClientKey(httpRequest))) {

			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
					.body(new ChatbotResponse(
							"Aap bahut tezi se messages bhej rahe hain. 🙏 Kripya thodi der ruk kar dobara try karein.",
							"RATE_LIMITED"));
		}

		try {
			ChatbotResponse response = chatbotService.getAnswer(request.getMessage(), request.getHistory());

			return ResponseEntity.ok(response);

		} catch (RuntimeException e) {
			// Do not leak internals; log only the exception type.
			log.error("Chatbot request failed: {}", e.getClass().getSimpleName());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ChatbotResponse(
							"Abhi chatbot mein kuch problem aa rahi hai. Kripya thodi der baad dobara try karein.",
							"UNAVAILABLE"));
		}
	}

	private String resolveClientKey(HttpServletRequest httpRequest) {

		if (trustForwardedHeader) {
			String forwarded = httpRequest.getHeader("X-Forwarded-For");
			if (forwarded != null && !forwarded.isBlank()) {
				return forwarded.split(",")[0].trim();
			}
		}

		return httpRequest.getRemoteAddr();
	}
}
