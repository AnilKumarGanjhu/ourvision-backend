package com.ourvision.controller;

import com.ourvision.dto.LoginRequest;
import com.ourvision.dto.LoginResponse;
import com.ourvision.repository.UserRepository;
import com.ourvision.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final UserRepository users;
	private final PasswordEncoder encoder;
	private final JwtService jwt;

	public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
		this.users = users;
		this.encoder = encoder;
		this.jwt = jwt;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest req) {
		var user = users.findByEmail(req.email()).filter(u -> "ACTIVE".equals(u.getStatus()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
		if (!encoder.matches(req.password(), user.getPassword()))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
		return new LoginResponse(jwt.generate(user.getEmail(), user.getRole().getName()), user.getId(),
				user.getFullName(), user.getRole().getName());
	}
}
