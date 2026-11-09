package com.ourvision.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ourvision.entity.Vote;

public interface VoteRepository extends JpaRepository<Vote, Long> {

	boolean existsByElectionIdAndVoterId(Long electionId, Long voterId);

	long countByElectionId(Long electionId);

	long countByElectionIdAndCandidateId(Long electionId, Long candidateId);

	/**
	 * Returns rows of [candidateId, voteCount] for a given election.
	 */
	@Query("""
			    SELECT v.candidateId, COUNT(v.id)
			    FROM Vote v
			    WHERE v.electionId = :electionId
			    GROUP BY v.candidateId
			""")
	List<Object[]> countVotesGroupedByCandidate(@Param("electionId") Long electionId);
}