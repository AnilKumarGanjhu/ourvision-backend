package com.ourvision.service;

/**
 * Thrown when Gemini cannot provide an answer. The message is internal only;
 * callers must show the user a friendly text based on {@link #getReason()}.
 */
public class GeminiUnavailableException extends Exception {

	private static final long serialVersionUID = 1L;

	public enum Reason {
		NOT_CONFIGURED, QUOTA_EXCEEDED, TIMEOUT, ACCESS_DENIED, BLOCKED, EMPTY_RESPONSE, UPSTREAM_ERROR
	}

	private final Reason reason;

	public GeminiUnavailableException(Reason reason, String internalMessage) {
		super(internalMessage);
		this.reason = reason;
	}

	public Reason getReason() {
		return reason;
	}
}
