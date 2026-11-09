package com.ourvision.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ourvision.entity.BusinessIdea;

public interface BusinessIdeaRepository
        extends JpaRepository<BusinessIdea, Long> {
}
