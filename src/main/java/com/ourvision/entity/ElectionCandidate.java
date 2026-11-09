package com.ourvision.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "election_candidates", uniqueConstraints = {
		@UniqueConstraint(name = "unique_candidate_per_election", columnNames = { "election_id", "user_id" }),
		@UniqueConstraint(name = "unique_candidate_id_election", columnNames = { "id", "election_id" }) })
public class ElectionCandidate {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "election_id", nullable = false)
	private Long electionId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(columnDefinition = "TEXT")
	private String manifesto;

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

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getManifesto() {
		return manifesto;
	}

	public void setManifesto(String manifesto) {
		this.manifesto = manifesto;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
