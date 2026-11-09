package com.ourvision.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ourvision.entity.Meeting;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

	long countByMeetingDateBetween(LocalDate from, LocalDate to);
}