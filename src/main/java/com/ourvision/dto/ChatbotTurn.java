package com.ourvision.dto;

/**
 * One earlier message of the conversation, sent by the frontend so Gemini can
 * answer follow-up questions. Role is "user" or "bot"/"model".
 */
public class ChatbotTurn {

	private String role;
	private String text;

	public ChatbotTurn() {
	}

	public ChatbotTurn(String role, String text) {
		this.role = role;
		this.text = text;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
