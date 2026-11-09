package com.ourvision.service;

import java.util.Map;

import com.ourvision.dto.ElectionResultResponse;

public interface ElectionResultService {

	/**
	 * Calculate + persist + publish results for an election. Idempotent: if already
	 * published, does nothing.
	 */
	void calculateAndPublishResults(Long electionId);

	/**
	 * Get published results (for frontend). If not yet published but election has
	 * ended, calculates first.
	 */
	ElectionResultResponse getPublishedResults(Long electionId);

	/**
	 * Winner info only.
	 */
	Map<String, Object> getWinner(Long electionId);
}