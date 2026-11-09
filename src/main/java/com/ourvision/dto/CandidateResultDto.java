package com.ourvision.dto;

public class CandidateResultDto {

	private Long candidateId;
	private Long userId;
	private String name;
	private String designation;
	private String photo;
	private String manifesto;
	private long voteCount;
	private double votePercentage;
	private int rank;
	private String resultStatus; // WINNER | RUNNER_UP | THIRD | TIE | CANDIDATE
	private boolean winner;

	public CandidateResultDto() {
	}

	public Long getCandidateId() {
		return candidateId;
	}

	public void setCandidateId(Long candidateId) {
		this.candidateId = candidateId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDesignation() {
		return designation;
	}

	public void setDesignation(String designation) {
		this.designation = designation;
	}

	public String getPhoto() {
		return photo;
	}

	public void setPhoto(String photo) {
		this.photo = photo;
	}

	public String getManifesto() {
		return manifesto;
	}

	public void setManifesto(String manifesto) {
		this.manifesto = manifesto;
	}

	public long getVoteCount() {
		return voteCount;
	}

	public void setVoteCount(long voteCount) {
		this.voteCount = voteCount;
	}

	public double getVotePercentage() {
		return votePercentage;
	}

	public void setVotePercentage(double votePercentage) {
		this.votePercentage = votePercentage;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
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
}