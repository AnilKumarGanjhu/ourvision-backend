package com.ourvision.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ourvision.entity.ProgramFeedback;

public interface ProgramFeedbackRepository extends JpaRepository<ProgramFeedback, Long> {
    List<ProgramFeedback> findByProgramIdOrderByCreatedAtDesc(Long programId);
    Optional<ProgramFeedback> findByProgramIdAndUserId(Long programId, Long userId);
}
