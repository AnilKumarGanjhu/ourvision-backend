package com.ourvision.repository;
import com.ourvision.entity.Contribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface ContributionRepository extends JpaRepository<Contribution, Long> {
    Page<Contribution> findByUser_Id(Long userId, Pageable pageable);
}
