package com.ourvision.dto;

import java.util.List;

public class ChatbotRequest {

	private String message;

	/** Optional: recent conversation turns, used only when Gemini is called. */
	private List<ChatbotTurn> history;

	public ChatbotRequest() {
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<ChatbotTurn> getHistory() {
		return history;
	}

	public void setHistory(List<ChatbotTurn> history) {
		this.history = history;
	}
}
