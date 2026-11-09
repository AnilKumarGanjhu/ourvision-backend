package com.ourvision.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "votes", uniqueConstraints = {
		// Ek member ek election mein sirf ek vote de sakta hai
		@UniqueConstraint(name = "uk_vote_election_voter", columnNames = { "election_id", "voter_id" }) }, indexes = {
				@Index(name = "idx_vote_election", columnList = "election_id"),
				@Index(name = "idx_vote_candidate", columnList = "candidate_id"),
				@Index(name = "idx_vote_election_candidate", columnList = "election_id, candidate_id") })
public class Vote {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "election_id", nullable = false)
	private Long electionId;

	@Column(name = "candidate_id", nullable = false)
	private Long candidateId;

	/**
	 * Member who cast the vote.
	 */
	@Column(name = "voter_id", nullable = false)
	private Long voterId;

	/**
	 * Optional: snapshot of member name at time of voting (audit).
	 */
	@Column(name = "voter_name", length = 150)
	private String voterName;

	@Column(name = "voted_at", nullable = false)
	private OffsetDateTime votedAt;

	@PrePersist
	protected void onCreate() {
		if (votedAt == null) {
			votedAt = OffsetDateTime.now();
		}
	}

	// ---------- Getters & Setters ----------

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

	public String getVoterName() {
		return voterName;
	}

	public void setVoterName(String voterName) {
		this.voterName = voterName;
	}

	public OffsetDateTime getVotedAt() {
		return votedAt;
	}

	public void setVotedAt(OffsetDateTime votedAt) {
		this.votedAt = votedAt;
	}
}