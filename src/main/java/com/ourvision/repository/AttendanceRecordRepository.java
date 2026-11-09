package com.ourvision.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ourvision.entity.AttendanceRecord;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
	List<AttendanceRecord> findByMeetingId(Long meetingId);

	List<AttendanceRecord> findByUserId(Long userId);

	List<AttendanceRecord> findByMeetingMeetingDateBetween(java.time.LocalDate from, java.time.LocalDate to);

	Optional<AttendanceRecord> findByMeetingIdAndUserId(Long meetingId, Long userId);
}