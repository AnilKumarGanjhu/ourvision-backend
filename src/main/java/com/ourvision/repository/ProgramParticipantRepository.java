package com.ourvision.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ourvision.entity.ProgramParticipant;

public interface ProgramParticipantRepository extends JpaRepository<ProgramParticipant, Long> {
    Optional<ProgramParticipant> findByProgramIdAndUserId(Long programId, Long userId);
    boolean existsByProgramIdAndUserId(Long programId, Long userId);
    List<ProgramParticipant> findByProgramId(Long programId);
}
