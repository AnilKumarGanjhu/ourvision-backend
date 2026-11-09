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
@Table(name = "election_votes", uniqueConstraints = {
		@UniqueConstraint(name = "unique_vote_per_member", columnNames = { "election_id", "voter_id" }) })
public class ElectionVote {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "election_id", nullable = false)
	private Long electionId;

	@Column(name = "candidate_id", nullable = false)
	private Long candidateId;

	@Column(name = "voter_id", nullable = false)
	private Long voterId;

	@Column(name = "voted_at", nullable = false)
	private OffsetDateTime votedAt;

	@PrePersist
	protected void onCreate() {
		if (votedAt == null) {
			votedAt = OffsetDateTime.now();
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

	public Long getCandidateId() {
		return candidateId;
	}

	public void setCandidateId(Long candidateId) {
		this.candidateId = candidateId;
	}

	public Long getVoterId() {
		return voterId;
	}

	public void setVoterId(Long voterId) {
		this.voterId = voterId;
	}

	public OffsetDateTime getVotedAt() {
		return votedAt;
	}
}
