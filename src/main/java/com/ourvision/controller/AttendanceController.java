package com.ourvision.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ourvision.dto.AttendanceBulkRequest;
import com.ourvision.serviceImpl.AttendanceServiceImpl;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
	private final AttendanceServiceImpl service;

	public AttendanceController(AttendanceServiceImpl s) {
		service = s;
	}

	@PostMapping("/bulk")
	public List<Map<String, Object>> bulk(@Valid @RequestBody AttendanceBulkRequest req, Authentication auth) {
		return service.bulk(req, auth.getName());
	}

	@GetMapping("/meeting/{meetingId}")
	public List<Map<String, Object>> byMeeting(@PathVariable Long meetingId) {
		return service.byMeeting(meetingId);
	}

	@GetMapping("/dashboard")
	public Map<String, Object> dashboard(@RequestParam(required = false) String month) {
		return service.dashboard(month == null ? LocalDate.now() : YearMonth.parse(month).atDay(1));
	}

	@GetMapping("/me")
	public List<Map<String, Object>> mine(Authentication auth) {
		return service.forUserEmail(auth.getName());
	}
}
