package com.ourvision.scheduler;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ourvision.entity.Election;
import com.ourvision.repository.ElectionRepository;
import com.ourvision.service.ElectionResultService;

@Component
public class ElectionResultScheduler {

	private static final Logger log = LoggerFactory.getLogger(ElectionResultScheduler.class);

	private final ElectionRepository electionRepository;
	private final ElectionResultService resultService;

	public ElectionResultScheduler(ElectionRepository electionRepository, ElectionResultService resultService) {
		this.electionRepository = electionRepository;
		this.resultService = resultService;
	}

	/**
	 * Runs every 60 seconds. Finds elections whose end time has passed but results
	 * not published.
	 */
	@Scheduled(fixedRate = 60_000L, initialDelay = 20_000L)
	public void autoPublishEndedElections() {
		OffsetDateTime now = OffsetDateTime.now();

		List<Election> ended = electionRepository.findEndedElectionsPendingResults(now);

		for (Election e : ended) {
			try {
				log.info("Auto-publishing results for election {}", e.getId());
				resultService.calculateAndPublishResults(e.getId());
			} catch (Exception ex) {
				log.error("Failed to auto-publish results for election {}: {}", e.getId(), ex.getMessage(), ex);
			}
		}
	}
}