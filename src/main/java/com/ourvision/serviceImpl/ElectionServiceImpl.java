
package com.ourvision.serviceImpl;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ourvision.dto.ElectionCandidateResponse;
import com.ourvision.entity.Election;
import com.ourvision.entity.ElectionAuditLog;
import com.ourvision.entity.ElectionCandidate;
import com.ourvision.entity.ElectionVote;
import com.ourvision.entity.User;
import com.ourvision.repository.ElectionAuditLogRepository;
import com.ourvision.repository.ElectionCandidateRepository;
import com.ourvision.repository.ElectionRepository;
import com.ourvision.repository.ElectionResultRepository;
import com.ourvision.repository.ElectionVoteRepository;
import com.ourvision.repository.UserRepository;
import com.ourvision.service.ElectionService;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ElectionServiceImpl implements ElectionService {

	// ✅ REPOSITORIES & SERVICES (Properly Injected)
	private final ElectionRepository electionRepository;
	private final ElectionCandidateRepository candidateRepository;
	private final ElectionVoteRepository voteRepository; // Actual votes
	private final ElectionAuditLogRepository auditLogRepository;
	private final ElectionResultRepository resultRepository; // Published results
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;

	public ElectionServiceImpl(ElectionRepository electionRepository, ElectionCandidateRepository candidateRepository,
			ElectionVoteRepository voteRepository, ElectionAuditLogRepository auditLogRepository,
			ElectionResultRepository resultRepository, UserRepository userRepository, ObjectMapper objectMapper) {

		this.electionRepository = electionRepository;
		this.candidateRepository = candidateRepository;
		this.voteRepository = voteRepository;
		this.auditLogRepository = auditLogRepository;
		this.resultRepository = resultRepository;
		this.userRepository = userRepository;
		this.objectMapper = objectMapper;
	}

	// 1. Get election by ID
	@Override
	@Transactional(readOnly = true)
	public Election getElection(Long electionId) {
		return electionRepository.findById(electionId)
				.orElseThrow(() -> new EntityNotFoundException("Election not found: " + electionId));
	}

	// 2. Get election by ID for controller/API usage
	@Override
	@Transactional(readOnly = true)
	public Election getElectionById(Long electionId) {
		return electionRepository.findById(electionId)
				.orElseThrow(() -> new EntityNotFoundException("Election not found with ID: " + electionId));
	}

	// 3. Get all elections
	@Override
	@Transactional(readOnly = true)
	public List<Election> getAllElections() {
		return electionRepository.findAllByOrderByCreatedAtDesc();
	}

	// 4. Create a new election
	@Override
	@Transactional
	public Election createElection(Election election, Long adminUserId) {

		if (election.getTitle() == null || election.getTitle().isBlank()) {
			throw new IllegalArgumentException("Election title is required");
		}

		if (election.getPositionName() == null || election.getPositionName().isBlank()) {
			throw new IllegalArgumentException("Position name is required");
		}

		if (election.getStartAt() == null || election.getEndAt() == null
				|| !election.getEndAt().isAfter(election.getStartAt())) {
			throw new IllegalArgumentException("Election end time must be after start time");
		}

		election.setStatus("DRAFT");
		election.setResultsPublished(false);
		election.setCreatedBy(adminUserId);

		Election savedElection = electionRepository.save(election);

		saveAuditLog(savedElection.getId(), adminUserId, "ELECTION_CREATED");

		return savedElection;
	}

	// 5. Get candidates for an election
	@Override
	@Transactional(readOnly = true)
	public List<ElectionCandidateResponse> getElectionCandidates(Long electionId) {

		getElection(electionId);

		List<ElectionCandidate> candidates = candidateRepository.findByElectionId(electionId);

		List<ElectionCandidateResponse> response = new ArrayList<>();

		for (ElectionCandidate candidate : candidates) {

			User user = userRepository.findById(candidate.getUserId()).orElse(null);

			String fullName = user != null ? user.getFullName() : null;
			String email = user != null ? user.getEmail() : null;

			ElectionCandidateResponse candidateResponse = new ElectionCandidateResponse(candidate.getId(),
					candidate.getElectionId(), candidate.getUserId(), fullName, email, candidate.getManifesto(),
					candidate.getCreatedAt());

			response.add(candidateResponse);
		}

		return response;
	}

	// 6. Add a candidate to an election
	@Override
	@Transactional
	public ElectionCandidate addCandidate(Long electionId, Long candidateUserId, String manifesto, Long adminUserId) {

		Election election = getElection(electionId);

		if (!"DRAFT".equals(election.getStatus())) {
			throw new IllegalStateException("Candidates can only be added to a DRAFT election");
		}

		if (candidateUserId == null) {
			throw new IllegalArgumentException("Candidate user ID is required");
		}

		if (candidateRepository.existsByElectionIdAndUserId(electionId, candidateUserId)) {
			throw new IllegalStateException("This user is already a candidate in this election");
		}

		ElectionCandidate candidate = new ElectionCandidate();
		candidate.setElectionId(electionId);
		candidate.setUserId(candidateUserId);
		candidate.setManifesto(manifesto);

		ElectionCandidate savedCandidate = candidateRepository.save(candidate);

		saveAuditLog(electionId, adminUserId, "CANDIDATE_ADDED");

		return savedCandidate;
	}

	// 7. Start an election
	@Override
	@Transactional
	public Election startElection(Long electionId, Long adminUserId) {

		Election election = getElection(electionId);
		OffsetDateTime now = OffsetDateTime.now();

		if (!"DRAFT".equals(election.getStatus()) && !"SCHEDULED".equals(election.getStatus())) {
			throw new IllegalStateException("Only DRAFT or SCHEDULED elections can be started");
		}

		if (now.isBefore(election.getStartAt())) {
			throw new IllegalStateException("Election start time has not arrived yet");
		}

		if (!now.isBefore(election.getEndAt())) {
			throw new IllegalStateException("Election end time has already passed");
		}

		if (candidateRepository.findByElectionId(electionId).isEmpty()) {
			throw new IllegalStateException("At least one candidate is required to start an election");
		}

		election.setStatus("LIVE");

		Election savedElection = electionRepository.save(election);

		saveAuditLog(electionId, adminUserId, "ELECTION_STARTED");

		return savedElection;
	}

	// 8. Cast a vote
	@Override
	@Transactional
	public ElectionVote castVote(Long electionId, Long candidateId, Long authenticatedUserId) {

		if (authenticatedUserId == null) {
			throw new IllegalArgumentException("Authenticated user is required");
		}

		if (candidateId == null) {
			throw new IllegalArgumentException("Candidate ID is required");
		}

		Election election = getElection(electionId);
		OffsetDateTime now = OffsetDateTime.now();

		if (!"LIVE".equals(election.getStatus())) {
			throw new IllegalStateException("Voting is not currently open");
		}

		if (now.isBefore(election.getStartAt()) || !now.isBefore(election.getEndAt())) {
			throw new IllegalStateException("Voting is outside the permitted election time");
		}

		ElectionCandidate candidate = candidateRepository.findById(candidateId)
				.orElseThrow(() -> new EntityNotFoundException("Candidate not found: " + candidateId));

		if (!candidate.getElectionId().equals(electionId)) {
			throw new IllegalArgumentException("Candidate does not belong to this election");
		}

		if (voteRepository.existsByElectionIdAndVoterId(electionId, authenticatedUserId)) {
			throw new IllegalStateException("You have already voted in this election");
		}

		ElectionVote vote = new ElectionVote();
		vote.setElectionId(electionId);
		vote.setCandidateId(candidateId);
		vote.setVoterId(authenticatedUserId);

		try {
			ElectionVote savedVote = voteRepository.saveAndFlush(vote);

			saveAuditLog(electionId, authenticatedUserId, "VOTE_CAST");

			return savedVote;

		} catch (DataIntegrityViolationException exception) {
			throw new IllegalStateException("Vote could not be recorded. You may have already voted.", exception);
		}
	}

	// 9. Close an election
	@Override
	@Transactional
	public Election closeElection(Long electionId, Long adminUserId) {

		Election election = getElection(electionId);

		if ("CLOSED".equals(election.getStatus())) {
			throw new IllegalStateException("Election is already closed");
		}

		if (!"LIVE".equals(election.getStatus())) {
			throw new IllegalStateException("Only a LIVE election can be closed");
		}

		election.setStatus("CLOSED");

		Election savedElection = electionRepository.save(election);

		saveAuditLog(electionId, adminUserId, "ELECTION_CLOSED");

		return savedElection;
	}

	// 10. Publish results after election closure
	@Override
	@Transactional
	public Election publishResults(Long electionId, Long adminUserId) {

		Election election = getElection(electionId);

		if (!"CLOSED".equals(election.getStatus())) {
			throw new IllegalStateException("Results can only be published after the election is closed");
		}

		if (election.isResultsPublished()) {
			throw new IllegalStateException("Election results have already been published");
		}

		election.setResultsPublished(true);

		Election savedElection = electionRepository.save(election);

		saveAuditLog(electionId, adminUserId, "RESULTS_PUBLISHED");

		return savedElection;
	}

	// 11. Get results only after closure and publication
	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> getResults(Long electionId) {

		Election election = getElection(electionId);

		if (!"CLOSED".equals(election.getStatus()) || !election.isResultsPublished()) {
			throw new IllegalStateException("Results are not available yet");
		}

		List<ElectionCandidate> candidates = candidateRepository.findByElectionId(electionId);

		List<Map<String, Object>> candidateResults = candidates.stream().map(candidate -> {
			Map<String, Object> row = new LinkedHashMap<>();

			row.put("candidateId", candidate.getId());
			row.put("candidateUserId", candidate.getUserId());
			row.put("votes", voteRepository.countByElectionIdAndCandidateId(electionId, candidate.getId()));

			return row;
		}).toList();

		Map<String, Object> result = new LinkedHashMap<>();

		result.put("electionId", electionId);
		result.put("title", election.getTitle());
		result.put("positionName", election.getPositionName());
		result.put("totalVotes", voteRepository.countByElectionId(electionId));
		result.put("candidates", candidateResults);

		return result;
	}

	// 12. Get recorded votes after election closure
	@Override
	@Transactional(readOnly = true)
	public List<ElectionVote> getRecordedVotes(Long electionId) {

		Election election = getElection(electionId);

		if (!"CLOSED".equals(election.getStatus())) {
			throw new IllegalStateException("Recorded voting details are available only after election closure");
		}

		return voteRepository.findByElectionIdOrderByVotedAtDesc(electionId);
	}

	// 13. Get audit logs
	@Override
	@Transactional(readOnly = true)
	public List<ElectionAuditLog> getAuditLogs(Long electionId) {

		getElection(electionId);

		return auditLogRepository.findByElectionIdOrderByCreatedAtDesc(electionId);
	}

	// 14. Save an audit log
	private void saveAuditLog(Long electionId, Long actorUserId, String action) {

		ElectionAuditLog auditLog = new ElectionAuditLog();

		auditLog.setElectionId(electionId);
		auditLog.setActorUserId(actorUserId);
		auditLog.setAction(action);

		ObjectNode details = objectMapper.createObjectNode();
		details.put("action", action);
		details.put("electionId", electionId);

		auditLog.setDetails(details);

		auditLogRepository.save(auditLog);
	}

	@Override
	@Transactional
	public void deleteElection(Long electionId, Long adminUserId) {
		Election election = electionRepository.findById(electionId)
				.orElseThrow(() -> new RuntimeException("Election not found with id: " + electionId));

		// Optional: Check if the user is actually an admin (Security layer handles this
		// usually)
		// User admin = userRepository.findById(adminUserId).orElseThrow();

		// 1. Delete Audit Logs
		auditLogRepository.deleteByElectionId(electionId);

		// 2. Delete Votes
		voteRepository.deleteByElectionId(electionId);

		// 3. Delete Results
		resultRepository.deleteByElectionId(electionId);

		// 4. Delete Candidates
		candidateRepository.deleteByElectionId(electionId);

		// 5. Finally, Delete the Election
		electionRepository.delete(election);
	}

	@Override
	@Transactional
	public Object updateCandidate(Long electionId, Long candidateId, Long newUserId, String newManifesto,
			Long adminUserId) {
		Election election = electionRepository.findById(electionId)
				.orElseThrow(() -> new RuntimeException("Election not found"));

		// 🚨 Rule Check: Election start ho chuki hai kya?
		if (!"DRAFT".equalsIgnoreCase(election.getStatus())) {
			throw new RuntimeException("Cannot update candidates. Election has already started or completed.");
		}

		ElectionCandidate candidate = candidateRepository.findById(candidateId)
				.orElseThrow(() -> new RuntimeException("Candidate not found"));

		// Naya user valid hai kya?
		User newUser = userRepository.findById(newUserId).orElseThrow(() -> new RuntimeException("New user not found"));

		candidate.setUserId(newUserId);
		candidate.setManifesto(newManifesto);

		candidateRepository.save(candidate);

		return candidate; // Ya jo DTO return karna ho
	}

	@Override
	@Transactional
	public void removeCandidate(Long electionId, Long candidateId, Long adminUserId) {
		Election election = electionRepository.findById(electionId)
				.orElseThrow(() -> new RuntimeException("Election not found"));

		// 🚨 Rule: Candidate sirf tabhi delete ho jab election DRAFT ho
		if (!"DRAFT".equalsIgnoreCase(election.getStatus())) {
			throw new RuntimeException("Cannot remove candidate. Election has already started.");
		}

		candidateRepository.deleteById(candidateId);
	}
}