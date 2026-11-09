package com.ourvision.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "election_results", uniqueConstraints = {
		@UniqueConstraint(name = "uk_result_election_candidate", columnNames = { "election_id",
				"candidate_id" }) }, indexes = { @Index(name = "idx_result_election", columnList = "election_id"),
						@Index(name = "idx_result_winner", columnList = "election_id, is_winner") })
public class ElectionResult {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "election_id", nullable = false)
	private Long electionId;

	/**
	 * References ElectionCandidate.id (NOT User.id)
	 */
	@Column(name = "candidate_id", nullable = false)
	private Long candidateId;

	@Column(name = "vote_count", nullable = false)
	private long voteCount = 0L;

	@Column(name = "total_votes", nullable = false)
	private long totalVotes = 0L;

	@Column(name = "vote_percentage", nullable = false)
	private double votePercentage = 0.0;

	@Column(name = "rank_position", nullable = false)
	private int rankPosition = 0;

	/**
	 * WINNER | RUNNER_UP | THIRD | TIE | CANDIDATE
	 */
	@Column(name = "result_status", nullable = false, length = 20)
	private String resultStatus = "CANDIDATE";

	@Column(name = "is_winner", nullable = false)
	private boolean winner = false;

	@Column(name = "is_tie", nullable = false)
	private boolean tie = false;

	@Column(name = "published", nullable = false)
	private boolean published = false;

	@Column(name = "calculated_at", nullable = false)
	private OffsetDateTime calculatedAt;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (calculatedAt == null) {
			calculatedAt = now;
		}
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = OffsetDateTime.now();
	}

	// Getters & Setters
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

	public long getVoteCount() {
		return voteCount;
	}

	public void setVoteCount(long voteCount) {
		this.voteCount = voteCount;
	}

	public long getTotalVotes() {
		return totalVotes;
	}

	public void setTotalVotes(long totalVotes) {
		this.totalVotes = totalVotes;
	}

	public double getVotePercentage() {
		return votePercentage;
	}

	public void setVotePercentage(double votePercentage) {
		this.votePercentage = votePercentage;
	}

	public int getRankPosition() {
		return rankPosition;
	}

	public void setRankPosition(int rankPosition) {
		this.rankPosition = rankPosition;
	}

	public String getResultStatus() {
		return resultStatus;
	}

	public void setResultStatus(String resultStatus) {
		this.resultStatus = resultStatus;
	}

	public boolean isWinner() {
		return winner;
	}

	public void setWinner(boolean winner) {
		this.winner = winner;
	}

	public boolean isTie() {
		return tie;
	}

	public void setTie(boolean tie) {
		this.tie = tie;
	}

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	public OffsetDateTime getCalculatedAt() {
		return calculatedAt;
	}

	public void setCalculatedAt(OffsetDateTime calculatedAt) {
		this.calculatedAt = calculatedAt;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}