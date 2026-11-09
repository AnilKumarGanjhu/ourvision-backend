package com.ourvision.controller;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ourvision.dto.ApiResponse;
import com.ourvision.dto.ElectionCandidateResponse;
import com.ourvision.dto.ElectionResultResponse;
import com.ourvision.entity.Election;
import com.ourvision.entity.ElectionAuditLog;
import com.ourvision.entity.ElectionVote;
import com.ourvision.entity.User;
import com.ourvision.repository.UserRepository;
import com.ourvision.service.ElectionResultService;
import com.ourvision.service.ElectionService;

@RestController
@RequestMapping("/api/elections")
public class ElectionController {

	private final ElectionService electionService;
	private final ElectionResultService electionResultService; // ← ADDED
	private final UserRepository userRepository;

	public ElectionController(ElectionService electionService, ElectionResultService electionResultService, // ← ADDED
			UserRepository userRepository) {
		this.electionService = electionService;
		this.electionResultService = electionResultService;
		this.userRepository = userRepository;
	}

	/**
	 * Create a new election. Only ADMIN users are allowed.
	 */
	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<?> createElection(@RequestBody CreateElectionRequest request,
			@AuthenticationPrincipal String authenticatedEmail) {

		Election election = new Election();

		election.setTitle(request.title());
		election.setDescription(request.description());
		election.setPositionName(request.positionName());
		election.setStartAt(request.startAt());
		election.setEndAt(request.endAt());

		Long adminUserId = requireUserId(authenticatedEmail);

		return ApiResponse.success(electionService.createElection(election, adminUserId));
	}

	/**
	 * Get all elections.
	 */
	@GetMapping
	public ApiResponse<List<Election>> getAllElections() {
		return ApiResponse.success(electionService.getAllElections());
	}

	/**
	 * Get a single election by ID.
	 */
	@GetMapping("/{electionId}")
	public ApiResponse<?> getElectionById(@PathVariable Long electionId) {

		return ApiResponse.success(electionService.getElectionById(electionId));
	}

	/**
	 * Add a candidate to an election. Only ADMIN users are allowed.
	 */
	@PostMapping("/{electionId}/candidates")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<?> addCandidate(@PathVariable Long electionId, @RequestBody AddCandidateRequest request,
			@AuthenticationPrincipal String authenticatedEmail) {

		Long adminUserId = requireUserId(authenticatedEmail);

		return ApiResponse.success(
				electionService.addCandidate(electionId, request.candidateUserId(), request.manifesto(), adminUserId));
	}

	/**
	 * Get candidates for an election.
	 */
	@GetMapping("/{electionId}/candidates")
	public ApiResponse<List<ElectionCandidateResponse>> getCandidates(@PathVariable Long electionId) {

		return ApiResponse.success(electionService.getElectionCandidates(electionId));
	}

	/**
	 * Start an election. Only ADMIN users are allowed.
	 */
	@PostMapping("/{electionId}/start")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<?> startElection(@PathVariable Long electionId,
			@AuthenticationPrincipal String authenticatedEmail) {

		Long adminUserId = requireUserId(authenticatedEmail);

		return ApiResponse.success(electionService.startElection(electionId, adminUserId));
	}

	/**
	 * Cast a vote. Authenticated users are allowed.
	 */
	@PostMapping("/{electionId}/vote")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<?> castVote(@PathVariable Long electionId, @RequestBody CastVoteRequest request,
			@AuthenticationPrincipal String authenticatedEmail) {

		Long authenticatedUserId = requireUserId(authenticatedEmail);

		return ApiResponse.success(electionService.castVote(electionId, request.candidateId(), authenticatedUserId));
	}

	/**
	 * Close an election. Only ADMIN users are allowed.
	 */
	@PostMapping("/{electionId}/close")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<?> closeElection(@PathVariable Long electionId,
			@AuthenticationPrincipal String authenticatedEmail) {

		Long adminUserId = requireUserId(authenticatedEmail);

		return ApiResponse.success(electionService.closeElection(electionId, adminUserId));
	}

	/**
	 * Publish election results. Only ADMIN users are allowed. NOTE: This is the
	 * MANUAL admin trigger. Auto-publish happens via ElectionResultScheduler.
	 */
	@PostMapping("/{electionId}/publish-results")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<?> publishResults(@PathVariable Long electionId,
			@AuthenticationPrincipal String authenticatedEmail) {

		Long adminUserId = requireUserId(authenticatedEmail);

		// New result service ko try karo (election_results table mein likhega)
		try {
			electionResultService.calculateAndPublishResults(electionId);
		} catch (Exception ex) {
			// Fallback: agar naya service fail ho to purana flow chalao
			return ApiResponse.success(electionService.publishResults(electionId, adminUserId));
		}

		return ApiResponse.success("Results published successfully");
	}

	/**
	 * Get election results.
	 *
	 * New behaviour: - If election has ended but results not yet calculated →
	 * auto-calculates - If results already published → returns
	 * ElectionResultResponse - If still ongoing → returns empty result with
	 * votingEnded=false
	 */
	@GetMapping("/{electionId}/results")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<ElectionResultResponse> getResults(@PathVariable Long electionId) {

		return ApiResponse.success(electionResultService.getPublishedResults(electionId));
	}

	/**
	 * Get recorded votes. Only ADMIN users are allowed.
	 */
	@GetMapping("/{electionId}/recorded-votes")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<List<ElectionVote>> getRecordedVotes(@PathVariable Long electionId,
			@AuthenticationPrincipal String authenticatedEmail) {

		requireUserId(authenticatedEmail);

		return ApiResponse.success(electionService.getRecordedVotes(electionId));
	}

	/**
	 * Get election audit logs. Only ADMIN users are allowed.
	 */
	@GetMapping("/{electionId}/audit-logs")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<List<ElectionAuditLog>> getAuditLogs(@PathVariable Long electionId,
			@AuthenticationPrincipal String authenticatedEmail) {

		requireUserId(authenticatedEmail);

		return ApiResponse.success(electionService.getAuditLogs(electionId));
	}

	/**
	 * Resolve the authenticated user's database ID from their email.
	 */
	private Long requireUserId(String authenticatedEmail) {

		if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
			throw new IllegalStateException("Authenticated user's email is missing.");
		}

		User user = userRepository.findByEmail(authenticatedEmail)
				.orElseThrow(() -> new IllegalStateException("Authenticated user was not found."));

		if (user.getId() == null) {
			throw new IllegalStateException("Authenticated user's ID is missing.");
		}

		return user.getId();
	}

	/**
	 * Request body for creating an election.
	 */
	public record CreateElectionRequest(String title, String description, String positionName, OffsetDateTime startAt,
			OffsetDateTime endAt) {
	}

	/**
	 * Request body for adding a candidate.
	 */
	public record AddCandidateRequest(Long candidateUserId, String manifesto) {
	}

	/**
	 * Request body for casting a vote.
	 */
	public record CastVoteRequest(Long candidateId) {
	}

	@DeleteMapping("/{electionId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<?> deleteElection(@PathVariable Long electionId,
			@AuthenticationPrincipal String authenticatedEmail) {

		Long adminUserId = requireUserId(authenticatedEmail);

		// Service ko call karein delete ke liye
		electionService.deleteElection(electionId, adminUserId);

		return ApiResponse.success("Election deleted successfully");
	}

	/**
	 * Update candidate details or Replace candidate. Only allowed if election is in
	 * DRAFT status.
	 */
	@PutMapping("/{electionId}/candidates/{candidateId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<?> updateCandidate(@PathVariable Long electionId, @PathVariable Long candidateId,
			@RequestBody AddCandidateRequest request, // reuse same request
			@AuthenticationPrincipal String authenticatedEmail) {

		Long adminUserId = requireUserId(authenticatedEmail);

		return ApiResponse.success(electionService.updateCandidate(electionId, candidateId, request.candidateUserId(),
				request.manifesto(), adminUserId));
	}

	@DeleteMapping("/{electionId}/candidates/{candidateId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<?> removeCandidate(@PathVariable Long electionId, @PathVariable Long candidateId,
			@AuthenticationPrincipal String authenticatedEmail) {

		Long adminUserId = requireUserId(authenticatedEmail);
		electionService.removeCandidate(electionId, candidateId, adminUserId);
		return ApiResponse.success("Candidate removed successfully");
	}
}