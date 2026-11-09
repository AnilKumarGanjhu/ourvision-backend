package com.ourvision.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ourvision.entity.Program;

public interface ProgramRepository extends JpaRepository<Program, Long> {
}
