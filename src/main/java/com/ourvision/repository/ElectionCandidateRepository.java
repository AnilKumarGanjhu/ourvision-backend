package com.ourvision.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ourvision.entity.ElectionCandidate;

public interface ElectionCandidateRepository extends JpaRepository<ElectionCandidate, Long> {

	List<ElectionCandidate> findByElectionId(Long electionId);

	Optional<ElectionCandidate> findByElectionIdAndUserId(Long electionId, Long userId);

	boolean existsByElectionIdAndUserId(Long electionId, Long userId);

	@Modifying
	@Query("DELETE FROM ElectionCandidate c WHERE c.electionId = :electionId")
	void deleteByElectionId(@Param("electionId") Long electionId);
}