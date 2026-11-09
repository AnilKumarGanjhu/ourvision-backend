package com.ourvision.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ourvision.entity.MonthlyDeposit;

public interface MonthlyDepositRepository extends JpaRepository<MonthlyDeposit, Long> {

	List<MonthlyDeposit> findByDepositMonth(LocalDate month);

	List<MonthlyDeposit> findByUserIdOrderByDepositMonthDesc(Long userId);

	Optional<MonthlyDeposit> findByUserIdAndDepositMonth(Long userId, LocalDate month);
}
