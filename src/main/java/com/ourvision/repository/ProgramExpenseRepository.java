package com.ourvision.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ourvision.entity.ProgramExpense;

public interface ProgramExpenseRepository extends JpaRepository<ProgramExpense, Long> {
}
