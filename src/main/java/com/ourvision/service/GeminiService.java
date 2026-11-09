package com.ourvision.service;

import java.util.List;

import com.ourvision.dto.ChatbotTurn;

/**
 * Server-side access to the Gemini API. The API key never leaves the backend.
 */
public interface GeminiService {

	/** True when an API key is configured and the integration is switched on. */
	boolean isEnabled();

	/**
	 * Generates an answer for the question.
	 *
	 * @param question the current user question
	 * @param history  optional earlier turns (may be null)
	 * @return the generated text, never blank
	 * @throws GeminiUnavailableException when no model could produce an answer
	 */
	String generateAnswer(String question, List<ChatbotTurn> history) throws GeminiUnavailableException;
}
