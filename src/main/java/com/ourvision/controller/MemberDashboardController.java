package com.ourvision.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ourvision.entity.User;
import com.ourvision.repository.UserRepository;
import com.ourvision.serviceImpl.AttendanceServiceImpl;
import com.ourvision.serviceImpl.DepositServiceImpl;

@RestController
@RequestMapping("/api/dashboard")
public class MemberDashboardController {
	private final UserRepository users;
	private final AttendanceServiceImpl attendance;
	private final DepositServiceImpl deposits;

	public MemberDashboardController(UserRepository u, AttendanceServiceImpl a, DepositServiceImpl d) {
		users = u;
		attendance = a;
		deposits = d;
	}

	@GetMapping("/me")
	public Map<String, Object> me(@RequestParam(required = false) String month, Authentication auth) {
		User user = users.findByEmail(auth.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		LocalDate m = month == null ? LocalDate.now() : YearMonth.parse(month).atDay(1);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("month", m.toString());
		result.put("attendance", attendance.memberDashboard(user.getId(), m));
		result.put("attendanceHistory", attendance.forUser(user.getId()));
		result.put("deposit", deposits.memberDashboard(user.getId(), m));
		result.put("depositHistory", deposits.forUser(user.getId()));
		return result;
	}
}
