package com.ourvision.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "deposit_settings")
public class DepositSetting {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "monthly_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal monthlyAmount;
	@Column(name = "effective_from", nullable = false)
	private LocalDate effectiveFrom;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private User createdBy;
	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	public DepositSetting() {
	}

	public Long getId() {
		return id;
	}

	public BigDecimal getMonthlyAmount() {
		return monthlyAmount;
	}

	public void setMonthlyAmount(BigDecimal v) {
		monthlyAmount = v;
	}

	public LocalDate getEffectiveFrom() {
		return effectiveFrom;
	}

	public void setEffectiveFrom(LocalDate v) {
		effectiveFrom = v;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User v) {
		createdBy = v;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
