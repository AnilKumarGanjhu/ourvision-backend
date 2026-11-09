package com.ourvision.dto;

public class WinnerDto {

	private Long candidateId;
	private Long userId;
	private String name;
	private String designation;
	private String photo;
	private long voteCount;
	private double votePercentage;

	public WinnerDto() {
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
}