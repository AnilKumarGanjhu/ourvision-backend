package com.ourvision.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "meeting_attendance", uniqueConstraints = @UniqueConstraint(name = "uk_meeting_attendance_member", columnNames = {
		"meeting_id", "user_id" }))
public class AttendanceRecord {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "meeting_id", nullable = false)
	private Meeting meeting;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
	@Column(nullable = false, length = 30)
	private String status = "PRESENT";
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "marked_by")
	private User markedBy;

	public AttendanceRecord() {
	}

	public Long getId() {
		return id;
	}

	public Meeting getMeeting() {
		return meeting;
	}

	public void setMeeting(Meeting v) {
		meeting = v;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User v) {
		user = v;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String v) {
		status = v;
	}

	public User getMarkedBy() {
		return markedBy;
	}

	public void setMarkedBy(User v) {
		markedBy = v;
	}
}
