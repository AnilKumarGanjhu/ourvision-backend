package com.ourvision.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ourvision.entity.DepositSetting;

public interface DepositSettingRepository extends JpaRepository<DepositSetting, Long> {
	Optional<DepositSetting> findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate date);
}