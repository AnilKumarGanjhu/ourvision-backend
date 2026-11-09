package com.ourvision.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ourvision.dto.DepositPaymentRequest;
import com.ourvision.dto.DepositSettingRequest;
import com.ourvision.serviceImpl.DepositServiceImpl;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/deposits")
public class DepositController {

	private final DepositServiceImpl service;

	public DepositController(DepositServiceImpl service) {
		this.service = service;
	}

	/**
	 * Organization-wide monthly deposit records. Accessible only to authorized
	 * office roles.
	 */
	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'PRESIDENT', 'SECRETARY', 'TREASURER')")
	public List<Map<String, Object>> month(@RequestParam(required = false) String month) {

		return service.monthRows(parseMonth(month));
	}

	/**
	 * Organization-wide monthly deposit dashboard.
	 */
	@GetMapping("/dashboard")
	@PreAuthorize("hasAnyRole('ADMIN', 'PRESIDENT', 'SECRETARY', 'TREASURER')")
	public Map<String, Object> dashboard(@RequestParam(required = false) String month) {

		return service.dashboard(parseMonth(month));
	}

	/**
	 * Manual deposit recording. This is not an online payment gateway verification
	 * endpoint.
	 */
	@PostMapping("/payment")
	@PreAuthorize("hasAnyRole('ADMIN', 'PRESIDENT', 'SECRETARY', 'TREASURER')")
	public Map<String, Object> payment(@Valid @RequestBody DepositPaymentRequest req, Authentication auth) {

		ensureAuthenticated(auth);
		return service.recordPayment(req, auth.getName());
	}

	/**
	 * Read monthly deposit settings. Any authenticated user can read the settings.
	 * Tighten this rule if settings should be staff-only.
	 */
	@GetMapping("/settings")
	@PreAuthorize("isAuthenticated()")
	public List<Map<String, Object>> settings() {
		return service.getSettings();
	}

	/**
	 * Only Admin can create a new deposit setting.
	 */
	@PutMapping("/settings")
	@PreAuthorize("hasRole('ADMIN')")
	public Map<String, Object> setSettings(@Valid @RequestBody DepositSettingRequest req, Authentication auth) {

		ensureAuthenticated(auth);
		return service.setSetting(req, auth.getName());
	}

	/**
	 * Deposit history belonging to the authenticated user.
	 */
	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	public List<Map<String, Object>> mine(Authentication auth) {
		ensureAuthenticated(auth);
		return service.forUserEmail(auth.getName());
	}

	private LocalDate parseMonth(String month) {
		if (month == null || month.isBlank()) {
			return LocalDate.now().withDayOfMonth(1);
		}

		try {
			return YearMonth.parse(month).atDay(1);
		} catch (DateTimeParseException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Invalid month format. Use yyyy-MM, for example 2026-11");
		}
	}

	private void ensureAuthenticated(Authentication auth) {
		if (auth == null || !auth.isAuthenticated() || auth.getName() == null || auth.getName().isBlank()
				|| "anonymousUser".equals(auth.getName())) {

			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
		}
	}
}
