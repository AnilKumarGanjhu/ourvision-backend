package com.ourvision.utill;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pure-Java helper used by the chatbot to decide whether a stored
 * knowledge-base question is "close enough" to what the user typed.
 *
 * It tolerates different capitalisation, punctuation, filler words
 * (English + Hinglish), simple plurals and small typos. It has no Spring or
 * database dependency so it can be unit tested in isolation.
 */
public final class ChatbotQuestionMatcher {

	/** Words that carry little meaning on their own (English + Hinglish). */
	private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
			// English
			"a", "an", "the", "is", "are", "was", "were", "be", "am", "do", "does", "did", "of", "to", "in", "on",
			"at", "for", "and", "or", "me", "my", "i", "you", "your", "we", "our", "us", "it", "its", "this", "that",
			"what", "whats", "which", "who", "whom", "how", "can", "could", "would", "will", "shall", "please", "pls",
			"tell", "about", "give", "know", "want", "need", "with", "by", "from", "as", "so", "if", "any",
			// Hinglish
			"kya", "hai", "hain", "ho", "hoon", "tha", "thi", "the", "ka", "ki", "ke", "ko", "se", "me", "mein", "mai",
			"par", "pe", "aur", "ya", "to", "toh", "bhi", "aap", "aapka", "aapki", "aapke", "hum", "hamara", "hamari",
			"hamare", "mera", "meri", "mere", "mujhe", "batao", "bataiye", "bataye", "bata", "kaise", "kaun", "kab",
			"kahan", "kyun", "kyu", "kitna", "kitni", "kitne", "ek", "yeh", "ye", "woh", "wo", "ji", "bhai", "yaar",
			"dost", "bro", "sir", "madam"));

	private static final int MAX_TYPO_DISTANCE_TOKEN_LENGTH = 5;

	private ChatbotQuestionMatcher() {
	}

	/** Lower-cases, strips accents and punctuation, collapses whitespace. */
	public static String normalize(String text) {
		if (text == null) {
			return "";
		}
		return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
	}

	/**
	 * Returns a similarity score between 0.0 and 1.0.
	 *
	 * @param userQuestion   the question typed by the user (raw or normalised)
	 * @param storedQuestion a question stored in the knowledge base
	 */
	public static double score(String userQuestion, String storedQuestion) {

		String user = normalize(userQuestion);
		String stored = normalize(storedQuestion);

		if (user.isEmpty() || stored.isEmpty()) {
			return 0.0;
		}

		if (user.equals(stored)) {
			return 1.0;
		}

		List<String> userTokens = meaningfulTokens(user);
		List<String> storedTokens = meaningfulTokens(stored);

		// Nothing meaningful to compare (e.g. only filler words).
		if (userTokens.isEmpty() || storedTokens.isEmpty()) {
			return 0.0;
		}

		int matched = countMatchedTokens(userTokens, storedTokens);

		if (matched == 0) {
			return 0.0;
		}

		double dice = (2.0 * matched) / (userTokens.size() + storedTokens.size());
		double score = dice;

		// Every keyword of a multi-keyword stored question appears in the user's
		// (longer) question, e.g. "please tell me the monthly deposit amount".
		if (storedTokens.size() >= 2 && matched == storedTokens.size()) {
			score = Math.max(score, 0.85);
		}

		// The user's multi-keyword question is fully contained in a longer stored one.
		if (userTokens.size() >= 2 && matched == userTokens.size()) {
			score = Math.max(score, 0.85);
		}

		return Math.min(score, 0.99);
	}

	private static int countMatchedTokens(List<String> userTokens, List<String> storedTokens) {

		boolean[] used = new boolean[storedTokens.size()];
		int matched = 0;

		for (String userToken : userTokens) {
			for (int i = 0; i < storedTokens.size(); i++) {
				if (!used[i] && tokensEqual(userToken, storedTokens.get(i))) {
					used[i] = true;
					matched++;
					break;
				}
			}
		}

		return matched;
	}

	private static boolean tokensEqual(String a, String b) {

		if (a.equals(b)) {
			return true;
		}

		// Allow one typo (edit distance 1) only for longer words.
		if (a.length() > MAX_TYPO_DISTANCE_TOKEN_LENGTH && b.length() > MAX_TYPO_DISTANCE_TOKEN_LENGTH
				&& Math.abs(a.length() - b.length()) <= 1) {
			return editDistanceAtMostOne(a, b);
		}

		return false;
	}

	private static List<String> meaningfulTokens(String normalized) {

		List<String> tokens = new ArrayList<>();

		for (String token : normalized.split(" ")) {
			if (token.isEmpty() || STOP_WORDS.contains(token)) {
				continue;
			}
			tokens.add(stem(token));
		}

		return tokens;
	}

	/** Very light plural stemming: "members" -> "member", "deposits" -> "deposit". */
	private static String stem(String token) {
		if (token.length() > 3 && token.endsWith("s") && !token.endsWith("ss")) {
			return token.substring(0, token.length() - 1);
		}
		return token;
	}

	private static boolean editDistanceAtMostOne(String a, String b) {

		if (a.equals(b)) {
			return true;
		}

		String shorter = a.length() <= b.length() ? a : b;
		String longer = a.length() <= b.length() ? b : a;

		int i = 0;
		int j = 0;
		boolean edited = false;

		while (i < shorter.length() && j < longer.length()) {
			if (shorter.charAt(i) == longer.charAt(j)) {
				i++;
				j++;
				continue;
			}

			if (edited) {
				return false;
			}
			edited = true;

			if (shorter.length() == longer.length()) {
				i++; // substitution
			}
			j++; // insertion in the longer string (or substitution)
		}

		return true;
	}
}
