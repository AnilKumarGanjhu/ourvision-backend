package com.ourvision.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ourvision.entity.ElectionVote;

public interface ElectionVoteRepository extends JpaRepository<ElectionVote, Long> {

	Optional<ElectionVote> findByElectionIdAndVoterId(Long electionId, Long voterId);

	boolean existsByElectionIdAndVoterId(Long electionId, Long voterId);

	List<ElectionVote> findByElectionIdOrderByVotedAtDesc(Long electionId);

	long countByElectionId(Long electionId);

	long countByElectionIdAndCandidateId(Long electionId, Long candidateId);

	@Modifying
	@Query("DELETE FROM ElectionVote v WHERE v.electionId = :electionId")
	void deleteByElectionId(@Param("electionId") Long electionId);
}
