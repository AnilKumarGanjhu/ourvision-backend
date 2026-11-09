package com.ourvision.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "monthly_deposits", uniqueConstraints = {
		@UniqueConstraint(name = "uk_monthly_deposit_user_month", columnNames = { "user_id", "deposit_month" }) })
public class MonthlyDeposit {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
	@Column(name = "deposit_month", nullable = false)
	private LocalDate depositMonth;
	@Column(name = "expected_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal expectedAmount;
	@Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal paidAmount = BigDecimal.ZERO;
	@Column(nullable = false, length = 20)
	private String status = "PENDING";
	@Column(name = "last_payment_date")
	private LocalDate lastPaymentDate;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recorded_by")
	private User recordedBy;
	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt = OffsetDateTime.now();

	@PreUpdate
	public void update() {
		updatedAt = OffsetDateTime.now();
	}

	public MonthlyDeposit() {
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User v) {
		user = v;
	}

	public LocalDate getDepositMonth() {
		return depositMonth;
	}

	public void setDepositMonth(LocalDate v) {
		depositMonth = v;
	}

	public BigDecimal getExpectedAmount() {
		return expectedAmount;
	}

	public void setExpectedAmount(BigDecimal v) {
		expectedAmount = v;
	}

	public BigDecimal getPaidAmount() {
		return paidAmount;
	}

	public void setPaidAmount(BigDecimal v) {
		paidAmount = v;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String v) {
		status = v;
	}

	public LocalDate getLastPaymentDate() {
		return lastPaymentDate;
	}

	public void setLastPaymentDate(LocalDate v) {
		lastPaymentDate = v;
	}

	public User getRecordedBy() {
		return recordedBy;
	}

	public void setRecordedBy(User v) {
		recordedBy = v;
	}
}
