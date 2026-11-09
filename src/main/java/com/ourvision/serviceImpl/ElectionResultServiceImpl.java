package com.ourvision.serviceImpl;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ourvision.dto.CandidateResultDto;
import com.ourvision.dto.ElectionResultResponse;
import com.ourvision.dto.WinnerDto;
import com.ourvision.entity.Election;
import com.ourvision.entity.ElectionCandidate;
import com.ourvision.entity.ElectionResult;
import com.ourvision.entity.User;
import com.ourvision.repository.ElectionCandidateRepository;
import com.ourvision.repository.ElectionRepository;
import com.ourvision.repository.ElectionResultRepository;
import com.ourvision.repository.UserRepository;
import com.ourvision.service.ElectionResultService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class ElectionResultServiceImpl implements ElectionResultService {

	private static final Logger log = LoggerFactory.getLogger(ElectionResultServiceImpl.class);

	private static final String STATUS_CLOSED = "CLOSED";
	private static final String STATUS_ENDED = "ENDED"; // alternate value support

	private final ElectionRepository electionRepository;
	private final ElectionCandidateRepository candidateRepository;
	private final ElectionResultRepository resultRepository;
	private final UserRepository userRepository;

	@PersistenceContext
	private EntityManager entityManager;

	public ElectionResultServiceImpl(ElectionRepository electionRepository,
			ElectionCandidateRepository candidateRepository, ElectionResultRepository resultRepository,
			UserRepository userRepository) {
		this.electionRepository = electionRepository;
		this.candidateRepository = candidateRepository;
		this.resultRepository = resultRepository;
		this.userRepository = userRepository;
	}

	@Override
	@Transactional
	public void calculateAndPublishResults(Long electionId) {
		Election election = electionRepository.findById(electionId)
				.orElseThrow(() -> new RuntimeException("Election not found: " + electionId));

		// Idempotency: if already published, skip
		if (election.isResultsPublished()) {
			log.info("Results already published for election {}", electionId);
			return;
		}

		// Ensure voting has ended
		if (!hasVotingEnded(election)) {
			log.warn("Election {} has not ended yet. Skipping result calculation.", electionId);
			return;
		}

		List<ElectionCandidate> candidates = candidateRepository.findByElectionId(electionId);
		if (candidates.isEmpty()) {
			log.warn("No candidates for election {}. Marking as published with empty results.", electionId);
			election.setResultsPublished(true);
			electionRepository.save(election);
			return;
		}

		// Step 1: Calculate votes using ElectionVote (actual voting table)
		Map<Long, Long> voteCountByCandidate = getVotesCountMap(electionId);
		long totalVotes = voteCountByCandidate.values().stream().mapToLong(Long::longValue).sum();

		// Step 2: build results
		List<ElectionResult> results = new ArrayList<>();
		for (ElectionCandidate candidate : candidates) {
			ElectionResult r = new ElectionResult();
			r.setElectionId(electionId);
			r.setCandidateId(candidate.getId());
			long votes = voteCountByCandidate.getOrDefault(candidate.getId(), 0L);
			r.setVoteCount(votes);
			r.setTotalVotes(totalVotes);
			r.setVotePercentage(totalVotes > 0 ? Math.round((votes * 10000.0 / totalVotes)) / 100.0 : 0.0);
			results.add(r);
		}

		// Step 3: sort by votes descending, then by candidateId ascending (stable
		// tie-break)
		results.sort(Comparator.comparingLong(ElectionResult::getVoteCount).reversed()
				.thenComparing(ElectionResult::getCandidateId));

		// Step 4: detect tie at top
		boolean topIsTie = results.size() >= 2 && results.get(0).getVoteCount() == results.get(1).getVoteCount()
				&& results.get(0).getVoteCount() > 0;

		// Step 5: assign ranks and statuses
		int rank = 1;
		for (int i = 0; i < results.size(); i++) {
			ElectionResult r = results.get(i);

			// rank: same-vote rows share the same rank
			if (i > 0 && results.get(i - 1).getVoteCount() == r.getVoteCount()) {
				// same rank as previous
			} else {
				rank = i + 1;
			}
			r.setRankPosition(rank);
			r.setPublished(true);

			// 1. Agar sabke 0 votes hain, to koi winner nahi
			if (results.get(0).getVoteCount() == 0) {
				r.setResultStatus("CANDIDATE");
				r.setWinner(false);
				r.setTie(false);
				continue;
			}

			// 2. Tie Case
			if (topIsTie && r.getVoteCount() == results.get(0).getVoteCount()) {
				r.setResultStatus("TIE");
				r.setWinner(false);
				r.setTie(true);
			}
			// 3. Winner Case
			else if (i == 0) {
				r.setResultStatus("WINNER");
				r.setWinner(true);
				r.setTie(false);
			}
			// 4. Baaki log
			else if (r.getRankPosition() == 2) {
				r.setResultStatus("RUNNER_UP");
				r.setWinner(false);
				r.setTie(false);
			} else if (r.getRankPosition() == 3) {
				r.setResultStatus("THIRD");
				r.setWinner(false);
				r.setTie(false);
			} else {
				r.setResultStatus("CANDIDATE");
				r.setWinner(false);
				r.setTie(false);
			}
		}

		// Step 6: clean old rows (safe re-run) and save
		resultRepository.deleteByElectionId(electionId);
		resultRepository.saveAll(results);

		// Step 7: mark election as published + closed
		election.setResultsPublished(true);
		election.setStatus(STATUS_CLOSED);
		electionRepository.save(election);

		log.info("✅ Results calculated & published for election {}. Total votes={}, tie={}", electionId, totalVotes,
				topIsTie);
	}

	@Override
	@Transactional
	public ElectionResultResponse getPublishedResults(Long electionId) {
		Election election = electionRepository.findById(electionId)
				.orElseThrow(() -> new RuntimeException("Election not found: " + electionId));

		// Lazy trigger: if election ended but results not calculated, do it now
		if (!election.isResultsPublished() && hasVotingEnded(election)) {
			log.info("Lazy calculation triggered for election {}", electionId);
			calculateAndPublishResults(electionId);
			election = electionRepository.findById(electionId).orElseThrow();
		}

		ElectionResultResponse response = new ElectionResultResponse();
		response.setElectionId(electionId);
		response.setElectionTitle(election.getTitle());
		response.setPositionName(election.getPositionName());
		response.setPublished(election.isResultsPublished());
		response.setVotingEnded(hasVotingEnded(election));
		response.setEndAt(election.getEndAt());

		if (!election.isResultsPublished()) {
			response.setResults(new ArrayList<>());
			response.setWinner(null);
			response.setTie(false);
			return response;
		}

		List<ElectionResult> results = resultRepository
				.findByElectionIdAndPublishedTrueOrderByRankPositionAsc(electionId);

		List<CandidateResultDto> dtos = new ArrayList<>();
		boolean tie = false;
		WinnerDto winner = null;

		for (ElectionResult r : results) {
			ElectionCandidate candidate = candidateRepository.findById(r.getCandidateId()).orElse(null);
			if (candidate == null) {
				continue;
			}

			User user = userRepository.findById(candidate.getUserId()).orElse(null);

			CandidateResultDto dto = new CandidateResultDto();
			dto.setCandidateId(candidate.getId());
			dto.setUserId(candidate.getUserId());
			dto.setName(user != null ? user.getFullName() : "Unknown");
			dto.setDesignation(user != null ? user.getDesignation() : null);
			dto.setPhoto(user != null ? (user.getImageUrl() != null ? user.getImageUrl() : user.getAvatar()) : null);
			dto.setManifesto(candidate.getManifesto());
			dto.setVoteCount(r.getVoteCount());
			dto.setVotePercentage(r.getVotePercentage());
			dto.setRank(r.getRankPosition());
			dto.setResultStatus(r.getResultStatus());
			dto.setWinner(r.isWinner());

			dtos.add(dto);

			if (r.isTie()) {
				tie = true;
			}
			if (r.isWinner() && winner == null) {
				winner = new WinnerDto();
				winner.setCandidateId(candidate.getId());
				winner.setUserId(candidate.getUserId());
				winner.setName(user != null ? user.getFullName() : "Unknown");
				winner.setDesignation(user != null ? user.getDesignation() : null);
				winner.setPhoto(
						user != null ? (user.getImageUrl() != null ? user.getImageUrl() : user.getAvatar()) : null);
				winner.setVoteCount(r.getVoteCount());
				winner.setVotePercentage(r.getVotePercentage());
			}
		}

		response.setResults(dtos);
		response.setWinner(winner);
		response.setTie(tie);
		response.setTotalVotes(results.isEmpty() ? 0L : results.get(0).getTotalVotes());

		return response;
	}

	@Override
	public Map<String, Object> getWinner(Long electionId) {
		Map<String, Object> result = new LinkedHashMap<>();
		ElectionResultResponse response = getPublishedResults(electionId);
		if (response.getWinner() != null) {
			result.put("winner", response.getWinner());
			result.put("tie", false);
		} else if (response.isTie()) {
			result.put("winner", null);
			result.put("tie", true);
			result.put("message", "Election ended in a tie");
		} else {
			result.put("winner", null);
			result.put("tie", false);
			result.put("message", "Results not available");
		}
		return result;
	}

	/**
	 * Native query to fetch vote counts directly from the actual voting table.
	 * Using EntityManager to bypass the unused Vote entity.
	 */
	@SuppressWarnings("unchecked")
	private Map<Long, Long> getVotesCountMap(Long electionId) {
		Map<Long, Long> map = new HashMap<>();

		// NOTE: Agar aapki table ka naam 'election_votes' hai to yahan wo naam aayega.
		// Agar 'election_votes' nahi chal raha to 'election_vote' (singular) try
		// karein.
		String sql = "SELECT candidate_id, COUNT(*) FROM election_votes WHERE election_id = :electionId GROUP BY candidate_id";

		try {
			List<Object[]> rows = entityManager.createNativeQuery(sql).setParameter("electionId", electionId)
					.getResultList();

			for (Object[] row : rows) {
				map.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
			}
		} catch (Exception e) {
			log.error("Error fetching votes from election_votes table. Fallback to empty. Error: {}", e.getMessage());
		}

		return map;
	}

	private boolean hasVotingEnded(Election election) {
		if (election.getEndAt() == null) {
			return false;
		}
		boolean timeEnded = election.getEndAt().isBefore(OffsetDateTime.now());
		boolean statusClosed = STATUS_CLOSED.equalsIgnoreCase(election.getStatus())
				|| STATUS_ENDED.equalsIgnoreCase(election.getStatus());
		return timeEnded || statusClosed;
	}
}