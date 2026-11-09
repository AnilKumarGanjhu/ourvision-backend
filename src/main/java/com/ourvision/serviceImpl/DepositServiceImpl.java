
package com.ourvision.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ourvision.dto.DepositPaymentRequest;
import com.ourvision.dto.DepositSettingRequest;
import com.ourvision.entity.DepositSetting;
import com.ourvision.entity.MonthlyDeposit;
import com.ourvision.entity.User;
import com.ourvision.repository.DepositSettingRepository;
import com.ourvision.repository.MonthlyDepositRepository;
import com.ourvision.repository.UserRepository;

@Service
public class DepositServiceImpl {

	private final MonthlyDepositRepository deposits;
	private final DepositSettingRepository settings;
	private final UserRepository users;

	public DepositServiceImpl(MonthlyDepositRepository deposits, DepositSettingRepository settings,
			UserRepository users) {
		this.deposits = deposits;
		this.settings = settings;
		this.users = users;
	}

	/**
	 * Creates a new monthly deposit setting. Admin authorization must also be
	 * enforced by the controller/security layer.
	 */
	@Transactional
	public Map<String, Object> setSetting(DepositSettingRequest req, String email) {
		if (req == null || req.monthlyAmount() == null || req.effectiveFrom() == null) {
			throw badRequest("Monthly amount and effective date are required");
		}

		BigDecimal monthlyAmount = req.monthlyAmount().setScale(2, RoundingMode.HALF_UP);

		if (monthlyAmount.signum() <= 0) {
			throw badRequest("Monthly amount must be greater than zero");
		}

		User actor = findUserByEmail(email);

		DepositSetting setting = new DepositSetting();
		setting.setMonthlyAmount(monthlyAmount);
		setting.setEffectiveFrom(req.effectiveFrom());
		setting.setCreatedBy(actor);

		settings.save(setting);

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("id", setting.getId());
		response.put("monthlyAmount", setting.getMonthlyAmount());
		response.put("effectiveFrom", setting.getEffectiveFrom());

		return response;
	}

	/**
	 * Returns deposit settings ordered by effective date, newest first. The
	 * controller must restrict access according to the application's policy.
	 */
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getSettings() {
		return settings.findAll().stream().sorted(Comparator.comparing(DepositSetting::getEffectiveFrom).reversed())
				.map(setting -> {
					Map<String, Object> row = new LinkedHashMap<>();
					row.put("id", setting.getId());
					row.put("monthlyAmount", setting.getMonthlyAmount());
					row.put("effectiveFrom", setting.getEffectiveFrom());
					return row;
				}).toList();
	}

	/**
	 * Creates missing monthly rows and returns the rows for the requested month. A
	 * database unique constraint on (user_id, deposit_month) is recommended to
	 * protect against simultaneous requests.
	 */
	@Transactional
	public List<Map<String, Object>> monthRows(LocalDate requestedMonth) {
		if (requestedMonth == null) {
			throw badRequest("Month is required");
		}

		LocalDate month = requestedMonth.withDayOfMonth(1);
		LocalDate monthEnd = month.plusMonths(1).minusDays(1);

		BigDecimal expected = findExpectedAmount(month);

		List<User> activeMembers = users.findAll().stream().filter(user -> "ACTIVE".equalsIgnoreCase(user.getStatus()))
				.filter(user -> user.getJoiningDate() == null || !user.getJoiningDate().isAfter(monthEnd)).toList();

		for (User user : activeMembers) {
			boolean exists = deposits.findByUserIdAndDepositMonth(user.getId(), month).isPresent();

			if (!exists) {
				MonthlyDeposit deposit = new MonthlyDeposit();
				deposit.setUser(user);
				deposit.setDepositMonth(month);
				deposit.setExpectedAmount(expected);
				deposit.setPaidAmount(BigDecimal.ZERO.setScale(2));
				deposit.setStatus("PENDING");

				deposits.save(deposit);
			}
		}

		return deposits.findByDepositMonth(month).stream()
				.sorted(Comparator.comparing(deposit -> deposit.getUser().getFullName(), String.CASE_INSENSITIVE_ORDER))
				.map(this::row).toList();
	}

	/**
	 * Records a manually received deposit payment.
	 *
	 * This method does not verify an online payment gateway transaction. The
	 * controller must authorize the caller before invoking this method.
	 */
	@Transactional
	public Map<String, Object> recordPayment(DepositPaymentRequest req, String email) {

		if (req == null || req.userId() == null || req.month() == null || req.amount() == null) {
			throw badRequest("User, month and amount are required");
		}

		BigDecimal amount = req.amount().setScale(2, RoundingMode.HALF_UP);

		if (amount.signum() <= 0) {
			throw badRequest("Payment amount must be greater than zero");
		}

		LocalDate month = req.month().withDayOfMonth(1);

		User actor = findUserByEmail(email);

		User member = users.findById(req.userId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		BigDecimal expected = findExpectedAmount(month);

		MonthlyDeposit deposit = deposits.findByUserIdAndDepositMonth(member.getId(), month).orElseGet(() -> {
			MonthlyDeposit newDeposit = new MonthlyDeposit();
			newDeposit.setUser(member);
			newDeposit.setDepositMonth(month);
			newDeposit.setExpectedAmount(expected);
			newDeposit.setPaidAmount(BigDecimal.ZERO.setScale(2));
			newDeposit.setStatus("PENDING");
			return newDeposit;
		});

		BigDecimal currentPaid = deposit.getPaidAmount() == null ? BigDecimal.ZERO.setScale(2)
				: deposit.getPaidAmount().setScale(2, RoundingMode.HALF_UP);

		BigDecimal newPaid = currentPaid.add(amount).setScale(2, RoundingMode.HALF_UP);

		BigDecimal depositExpected = deposit.getExpectedAmount();

		if (depositExpected == null || depositExpected.signum() < 0) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Invalid expected deposit amount configured for this record");
		}

		if (newPaid.compareTo(depositExpected) > 0) {
			throw badRequest("Payment exceeds expected monthly amount");
		}

		deposit.setPaidAmount(newPaid);
		deposit.setLastPaymentDate(req.paymentDate() == null ? LocalDate.now() : req.paymentDate());
		deposit.setRecordedBy(actor);

		if (newPaid.compareTo(depositExpected) >= 0) {
			deposit.setStatus("PAID");
		} else if (newPaid.signum() > 0) {
			deposit.setStatus("PARTIAL");
		} else {
			deposit.setStatus("PENDING");
		}

		deposits.save(deposit);

		return row(deposit);
	}

	/**
	 * Returns deposit history for the authenticated member's email.
	 */
	@Transactional(readOnly = true)
	public List<Map<String, Object>> forUserEmail(String email) {
		User user = findUserByEmail(email);
		return forUser(user.getId());
	}

	/**
	 * Returns deposit history for a given user ID.
	 *
	 * IMPORTANT: This method does not perform authorization itself. Only call it
	 * from a controller that has verified the caller is permitted to access this
	 * user's records.
	 */
	@Transactional(readOnly = true)
	public List<Map<String, Object>> forUser(Long userId) {
		if (userId == null) {
			throw badRequest("User ID is required");
		}

		return deposits.findByUserIdOrderByDepositMonthDesc(userId).stream().map(this::row).toList();
	}

	/**
	 * Organization-wide monthly dashboard. The controller must restrict this method
	 * to authorized roles.
	 */
	@Transactional
	public Map<String, Object> dashboard(LocalDate requestedMonth) {
		if (requestedMonth == null) {
			throw badRequest("Month is required");
		}

		LocalDate month = requestedMonth.withDayOfMonth(1);
		List<Map<String, Object>> rows = monthRows(month);

		BigDecimal expectedTotal = BigDecimal.ZERO.setScale(2);
		BigDecimal paidTotal = BigDecimal.ZERO.setScale(2);

		for (Map<String, Object> row : rows) {
			expectedTotal = expectedTotal.add((BigDecimal) row.get("expectedAmount"));
			paidTotal = paidTotal.add((BigDecimal) row.get("paidAmount"));
		}

		BigDecimal pendingTotal = expectedTotal.subtract(paidTotal).max(BigDecimal.ZERO).setScale(2,
				RoundingMode.HALF_UP);

		double percentage = expectedTotal.signum() == 0 ? 0.0
				: paidTotal.multiply(BigDecimal.valueOf(100)).divide(expectedTotal, 2, RoundingMode.HALF_UP)
						.doubleValue();

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("month", month.toString());
		response.put("memberCount", rows.size());
		response.put("expectedAmount", expectedTotal);
		response.put("collectedAmount", paidTotal);
		response.put("pendingAmount", pendingTotal);
		response.put("percentage", percentage);
		response.put("members", rows);

		return response;
	}

	/**
	 * Returns a member's dashboard for the specified month.
	 *
	 * The controller must derive or validate userId against the authenticated
	 * identity. Do not trust an arbitrary userId supplied by a member.
	 */
	@Transactional(readOnly = true)
	public Map<String, Object> memberDashboard(Long userId, LocalDate requestedMonth) {

		if (userId == null || requestedMonth == null) {
			throw badRequest("User ID and month are required");
		}

		LocalDate month = requestedMonth.withDayOfMonth(1);

		return deposits.findByUserIdAndDepositMonth(userId, month).map(this::row).orElseGet(() -> {
			BigDecimal expected = settings.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(month)
					.map(DepositSetting::getMonthlyAmount).orElse(BigDecimal.ZERO.setScale(2));

			BigDecimal pending = expected.max(BigDecimal.ZERO);

			Map<String, Object> response = new LinkedHashMap<>();
			response.put("userId", userId);
			response.put("month", month.toString());
			response.put("expectedAmount", expected);
			response.put("paidAmount", BigDecimal.ZERO.setScale(2));
			response.put("pendingAmount", pending);
			response.put("percentage", 0.0);
			response.put("status", "PENDING");

			return response;
		});
	}

	private User findUserByEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
		}

		return users.findByEmail(email).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
	}

	private BigDecimal findExpectedAmount(LocalDate month) {
		return settings.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(month)
				.map(DepositSetting::getMonthlyAmount)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Configure a monthly deposit amount effective for this month first"));
	}

	private Map<String, Object> row(MonthlyDeposit deposit) {
		BigDecimal expected = deposit.getExpectedAmount() == null ? BigDecimal.ZERO.setScale(2)
				: deposit.getExpectedAmount();

		BigDecimal paid = deposit.getPaidAmount() == null ? BigDecimal.ZERO.setScale(2) : deposit.getPaidAmount();

		BigDecimal pending = expected.subtract(paid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

		double percentage = expected.signum() <= 0 ? 0.0
				: paid.multiply(BigDecimal.valueOf(100)).divide(expected, 2, RoundingMode.HALF_UP).doubleValue();

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("id", deposit.getId());
		response.put("userId", deposit.getUser().getId());
		response.put("userName", deposit.getUser().getFullName());
		response.put("month", deposit.getDepositMonth().toString());
		response.put("expectedAmount", expected);
		response.put("paidAmount", paid);
		response.put("pendingAmount", pending);
		response.put("percentage", percentage);
		response.put("status", deposit.getStatus());
		response.put("lastPaymentDate", deposit.getLastPaymentDate());

		return response;
	}

	private ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}
}
