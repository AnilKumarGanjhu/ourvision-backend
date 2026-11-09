package com.ourvision.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ourvision.dto.ContributionRequest;
import com.ourvision.entity.Contribution;
import com.ourvision.entity.User;
import com.ourvision.repository.ContributionRepository;
import com.ourvision.repository.UserRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contributions")
public class ContributionController {
	private static final Set<String> FINANCE_ROLES = Set.of("ROLE_ADMIN", "ROLE_PRESIDENT", "ROLE_SECRETARY",
			"ROLE_TREASURER");

	private final ContributionRepository contributions;
	private final UserRepository users;

	public ContributionController(ContributionRepository contributions, UserRepository users) {
		this.contributions = contributions;
		this.users = users;
	}

	private boolean canManageFinance(Authentication auth) {
		return auth != null
				&& auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(FINANCE_ROLES::contains);
	}

	private User currentUser(Authentication auth) {
		if (auth == null || auth.getName() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}
		return users.findByEmail(auth.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
	}

	private Map<String, Object> view(Contribution c) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", c.getId());
		m.put("userId", c.getUser().getId());
		m.put("memberName", c.getUser().getFullName());
		m.put("amount", c.getAmount());
		m.put("month", c.getContributionMonth());
		m.put("paymentMode", c.getPaymentMode());
		m.put("receiptNo", c.getReceiptNo());
		m.put("late", c.isLate());
		m.put("fineAmount", c.getFineAmount());
		m.put("paidAt", c.getPaidAt());
		return m;
	}

	/** Full contribution listing is restricted to finance/admin office-bearers. */
	@GetMapping
	public List<Map<String, Object>> all(Authentication auth) {
		if (!canManageFinance(auth)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to view all contributions");
		}
		return contributions.findAll().stream().map(this::view).toList();
	}

	/**
	 * Finance roles can filter by member; other authenticated users can only see
	 * their own records.
	 */
	@GetMapping("/page")
	public Page<Map<String, Object>> page(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "25") int size, @RequestParam(required = false) Long userId,
			Authentication auth) {
		int safePage = Math.max(0, page);
		int safeSize = Math.max(1, Math.min(size, 100));
		var pageable = PageRequest.of(safePage, safeSize, Sort.by("id").descending());
		if (canManageFinance(auth)) {
			var result = userId == null ? contributions.findAll(pageable)
					: contributions.findByUser_Id(userId, pageable);
			return result.map(this::view);
		}
		Long ownId = currentUser(auth).getId();
		return contributions.findByUser_Id(ownId, pageable).map(this::view);
	}

	/**
	 * Manual contribution entry is an administrative recording action, not online
	 * payment verification.
	 */
	@PostMapping
	public Map<String, Object> create(@Valid @RequestBody ContributionRequest req, Authentication auth) {
		if (!canManageFinance(auth)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to record manual contributions");
		}
		var user = users.findById(req.userId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member not found"));
		Contribution c = new Contribution();
		c.setUser(user);
		c.setAmount(req.amount());
		c.setContributionMonth(req.month());
		c.setPaymentMode(req.paymentMode().toUpperCase(Locale.ROOT));
		c.setReceiptNo(req.receiptNo());
		LocalDate today = LocalDate.now();
		boolean late = today.getDayOfMonth() > 10;
		c.setLate(late);
		c.setFineAmount(late ? BigDecimal.valueOf(50) : BigDecimal.ZERO);
		return view(contributions.save(c));
	}
}
