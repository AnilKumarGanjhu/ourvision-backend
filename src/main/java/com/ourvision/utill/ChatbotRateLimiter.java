package com.ourvision.utill;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Small in-memory sliding-window limiter. /api/chatbot/** is public, so this
 * stops anonymous callers from burning the Gemini quota.
 */
@Component
public class ChatbotRateLimiter {

	private static final int CLEANUP_THRESHOLD = 5000;

	private final int maxRequests;
	private final long windowMillis;
	private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

	public ChatbotRateLimiter(@Value("${chatbot.rate-limit.max-requests:20}") int maxRequests,
			@Value("${chatbot.rate-limit.window-seconds:60}") int windowSeconds) {
		this.maxRequests = maxRequests;
		this.windowMillis = Math.max(1, windowSeconds) * 1000L;
	}

	/** @return true if the caller may proceed, false if the limit is exceeded. */
	public boolean tryAcquire(String clientKey) {

		if (maxRequests <= 0) {
			return true; // limiter disabled
		}

		long now = System.currentTimeMillis();

		if (hits.size() > CLEANUP_THRESHOLD) {
			hits.entrySet().removeIf(entry -> isStale(entry.getValue(), now));
		}

		Deque<Long> window = hits.computeIfAbsent(clientKey == null ? "unknown" : clientKey, key -> new ArrayDeque<>());

		synchronized (window) {
			while (!window.isEmpty() && now - window.peekFirst() > windowMillis) {
				window.pollFirst();
			}

			if (window.size() >= maxRequests) {
				return false;
			}

			window.addLast(now);
			return true;
		}
	}

	private boolean isStale(Deque<Long> window, long now) {
		synchronized (window) {
			return window.isEmpty() || now - window.peekLast() > windowMillis;
		}
	}
}
