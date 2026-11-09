package com.ourvision.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ourvision.entity.Election;

public interface ElectionRepository extends JpaRepository<Election, Long> {

	List<Election> findByStatusOrderByStartAtDesc(String status);

	List<Election> findAllByOrderByCreatedAtDesc();

	@Query("""
			    SELECT e FROM Election e
			    WHERE e.endAt <= :now
			      AND e.resultsPublished = false
			      AND (e.status = 'ACTIVE' OR e.status = 'ONGOING' OR e.status = 'DRAFT')
			""")
	List<Election> findEndedElectionsPendingResults(OffsetDateTime now);
}
