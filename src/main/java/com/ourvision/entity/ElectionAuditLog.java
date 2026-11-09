
package com.ourvision.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "election_audit_logs")
public class ElectionAuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "election_id", nullable = false)
	private Long electionId;

	@Column(name = "actor_user_id")
	private Long actorUserId;

	@Column(nullable = false, length = 50)
	private String action;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private JsonNode details;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		if (createdAt == null) {
			createdAt = OffsetDateTime.now();
		}
	}

	public Long getId() {
		return id;
	}

	public Long getElectionId() {
		return electionId;
	}

	public void setElectionId(Long electionId) {
		this.electionId = electionId;
	}

	public Long getActorUserId() {
		return actorUserId;
	}

	public void setActorUserId(Long actorUserId) {
		this.actorUserId = actorUserId;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public JsonNode getDetails() {
		return details;
	}

	public void setDetails(JsonNode details) {
		this.details = details;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}