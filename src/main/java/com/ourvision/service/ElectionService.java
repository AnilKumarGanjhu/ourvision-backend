package com.ourvision.service;

import java.util.List;
import java.util.Map;

import com.ourvision.dto.ElectionCandidateResponse;
import com.ourvision.entity.Election;
import com.ourvision.entity.ElectionAuditLog;
import com.ourvision.entity.ElectionCandidate;
import com.ourvision.entity.ElectionVote;

import jakarta.validation.constraints.NotNull;

public interface ElectionService {

	List<Election> getAllElections();

	Election getElection(Long electionId);

	List<ElectionAuditLog> getAuditLogs(Long electionId);

	List<ElectionVote> getRecordedVotes(Long electionId);

	Map<String, Object> getResults(Long electionId);

	Election publishResults(Long electionId, Long requireUserId);

	Election closeElection(Long electionId, Long requireUserId);

	ElectionVote castVote(Long electionId, Long candidateId, Long requireUserId);

	Election startElection(Long electionId, Long requireUserId);

	Election createElection(Election election, Long requireUserId);

	ElectionCandidate addCandidate(Long electionId,
			@NotNull(message = "Candidate user ID is required") Long candidateUserId, String manifesto,
			Long requireUserId);

	List<ElectionCandidateResponse> getElectionCandidates(Long electionId);

	Election getElectionById(Long electionId);

	void deleteElection(Long electionId, Long adminUserId);

	void removeCandidate(Long electionId, Long candidateId, Long adminUserId);

	Object updateCandidate(Long electionId, Long candidateId, Long candidateUserId, String manifesto, Long adminUserId);

}
