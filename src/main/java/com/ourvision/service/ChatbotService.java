package com.ourvision.service;

import java.util.List;

import com.ourvision.dto.ChatbotResponse;
import com.ourvision.dto.ChatbotTurn;

public interface ChatbotService {

	/** Database first, Gemini as fallback. */
	ChatbotResponse getAnswer(String message, List<ChatbotTurn> history);

	default ChatbotResponse getAnswer(String message) {
		return getAnswer(message, null);
	}

}
