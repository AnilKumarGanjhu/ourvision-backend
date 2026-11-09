package com.ourvision.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ourvision.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

	private final JwtService jwt;
	private final UserRepository users;

	public JwtAuthFilter(JwtService jwt, UserRepository users) {
		this.jwt = jwt;
		this.users = users;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
			throws ServletException, IOException {

		String header = req.getHeader("Authorization");

		if (header != null && header.startsWith("Bearer ")) {
			String token = header.substring(7);
			String where = req.getMethod() + " " + req.getRequestURI();

			try {
				if (!jwt.valid(token)) {
					log.warn("JWT rejected for {}: invalid or expired token", where);
				} else {
					String email = jwt.subject(token);

					users.findByEmail(email).ifPresentOrElse(u -> {
						if (!"ACTIVE".equals(u.getStatus())) {
							log.warn("JWT user {} has status '{}' for {}, treated as anonymous", email, u.getStatus(),
									where);
							return;
						}

						String roleName = u.getRole() == null ? "" : u.getRole().getName();

						if (roleName == null || roleName.isBlank()) {
							log.warn("User {} has no role, authenticated without authorities for {}", email, where);
						} else if (!roleName.startsWith("ROLE_")) {
							roleName = "ROLE_" + roleName;
						}

						var auth = new UsernamePasswordAuthenticationToken(u.getEmail(), null,
								(roleName == null || roleName.isBlank()) ? List.of()
										: List.of(new SimpleGrantedAuthority(roleName)));

						SecurityContextHolder.getContext().setAuthentication(auth);

						log.debug("Authenticated {} with {} for {}", email, roleName, where);
					}, () -> log.warn("JWT subject {} not found in users table for {}", email, where));
				}
			} catch (RuntimeException ex) {
				log.error("JWT processing failed for {}", where, ex);
				SecurityContextHolder.clearContext();
			}
		}

		chain.doFilter(req, res);
	}
}