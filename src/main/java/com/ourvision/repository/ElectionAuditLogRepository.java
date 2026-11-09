package com.ourvision.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ourvision.entity.ElectionAuditLog;

public interface ElectionAuditLogRepository extends JpaRepository<ElectionAuditLog, Long> {

	List<ElectionAuditLog> findByElectionIdOrderByCreatedAtDesc(Long electionId);

	List<ElectionAuditLog> findByActorUserIdOrderByCreatedAtDesc(Long actorUserId);

	@Modifying
	@Query("DELETE FROM ElectionAuditLog a WHERE a.electionId = :electionId")
	void deleteByElectionId(@Param("electionId") Long electionId);
}
