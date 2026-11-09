
package com.ourvision.dto;

public class ChatbotResponse {

	private String answer;
	private String source;

	public ChatbotResponse() {
	}

	public ChatbotResponse(String answer, String source) {
		this.answer = answer;
		this.source = source;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
}
