package com.ourvision.dto;

import java.time.OffsetDateTime;

public class ElectionCandidateResponse {

	private Long id;
	private Long electionId;
	private Long userId;
	private String fullName;
	private String email;
	private String manifesto;
	private OffsetDateTime createdAt;

	public ElectionCandidateResponse() {
	}

	public ElectionCandidateResponse(Long id, Long electionId, Long userId, String fullName, String email,
			String manifesto, OffsetDateTime createdAt) {
		this.id = id;
		this.electionId = electionId;
		this.userId = userId;
		this.fullName = fullName;
		this.email = email;
		this.manifesto = manifesto;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public Long getElectionId() {
		return electionId;
	}

	public Long getUserId() {
		return userId;
	}

	public String getFullName() {
		return fullName;
	}

	public String getEmail() {
		return email;
	}

	public String getManifesto() {
		return manifesto;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setElectionId(Long electionId) {
		this.electionId = electionId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setManifesto(String manifesto) {
		this.manifesto = manifesto;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
