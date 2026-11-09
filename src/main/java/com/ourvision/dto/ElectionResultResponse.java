package com.ourvision.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class ElectionResultResponse {

	private Long electionId;
	private String electionTitle;
	private String positionName;
	private boolean published;
	private boolean votingEnded;
	private boolean tie;
	private long totalVotes;
	private OffsetDateTime endAt;
	private WinnerDto winner;
	private List<CandidateResultDto> results = new ArrayList<>();

	public ElectionResultResponse() {
	}

	public Long getElectionId() {
		return electionId;
	}

	public void setElectionId(Long electionId) {
		this.electionId = electionId;
	}

	public String getElectionTitle() {
		return electionTitle;
	}

	public void setElectionTitle(String electionTitle) {
		this.electionTitle = electionTitle;
	}

	public String getPositionName() {
		return positionName;
	}

	public void setPositionName(String positionName) {
		this.positionName = positionName;
	}

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	public boolean isVotingEnded() {
		return votingEnded;
	}

	public void setVotingEnded(boolean votingEnded) {
		this.votingEnded = votingEnded;
	}

	public boolean isTie() {
		return tie;
	}

	public void setTie(boolean tie) {
		this.tie = tie;
	}

	public long getTotalVotes() {
		return totalVotes;
	}

	public void setTotalVotes(long totalVotes) {
		this.totalVotes = totalVotes;
	}

	public OffsetDateTime getEndAt() {
		return endAt;
	}

	public void setEndAt(OffsetDateTime endAt) {
		this.endAt = endAt;
	}

	public WinnerDto getWinner() {
		return winner;
	}

	public void setWinner(WinnerDto winner) {
		this.winner = winner;
	}

	public List<CandidateResultDto> getResults() {
		return results;
	}

	public void setResults(List<CandidateResultDto> results) {
		this.results = results;
	}
}