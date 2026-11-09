package com.ourvision.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ourvision.entity.ElectionResult;

public interface ElectionResultRepository extends JpaRepository<ElectionResult, Long> {

	List<ElectionResult> findByElectionIdOrderByRankPositionAsc(Long electionId);

	List<ElectionResult> findByElectionIdAndPublishedTrueOrderByRankPositionAsc(Long electionId);

	Optional<ElectionResult> findByElectionIdAndCandidateId(Long electionId, Long candidateId);

	Optional<ElectionResult> findByElectionIdAndWinnerTrue(Long electionId);

	boolean existsByElectionIdAndPublishedTrue(Long electionId);

	@Modifying
	@Query("DELETE FROM ElectionResult r WHERE r.electionId = :electionId")
	void deleteByElectionId(@Param("electionId") Long electionId);
}