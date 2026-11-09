package com.ourvision.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "elections")
public class Election {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "position_name", nullable = false, length = 100)
	private String positionName;

	@Column(name = "start_at", nullable = false)
	private OffsetDateTime startAt;

	@Column(name = "end_at", nullable = false)
	private OffsetDateTime endAt;

	@Column(nullable = false, length = 20)
	private String status = "DRAFT";

	@Column(name = "results_published", nullable = false)
	private boolean resultsPublished = false;

	@Column(name = "created_by")
	private Long createdBy;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		OffsetDateTime now = OffsetDateTime.now();
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

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPositionName() {
		return positionName;
	}

	public void setPositionName(String positionName) {
		this.positionName = positionName;
	}

	public OffsetDateTime getStartAt() {
		return startAt;
	}

	public void setStartAt(OffsetDateTime startAt) {
		this.startAt = startAt;
	}

	public OffsetDateTime getEndAt() {
		return endAt;
	}

	public void setEndAt(OffsetDateTime endAt) {
		this.endAt = endAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public boolean isResultsPublished() {
		return resultsPublished;
	}

	public void setResultsPublished(boolean resultsPublished) {
		this.resultsPublished = resultsPublished;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
