package com.ourvision.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ourvision.security.JwtAuthFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthFilter jwtAuthFilter;

	public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
		this.jwtAuthFilter = jwtAuthFilter;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http.cors(cors -> cors.configurationSource(corsConfigurationSource())).csrf(csrf -> csrf.disable())

				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				.authorizeHttpRequests(auth -> auth

						// Public authentication endpoints
						.requestMatchers("/api/auth/**", "/error", "/swagger-ui/**", "/v3/api-docs/**",
								"/actuator/health")
						.permitAll()

						// Public uploads
						.requestMatchers("/uploads/**").permitAll()

						// Member Information photo - ADMIN only
						// .requestMatchers(HttpMethod.GET,
						// "/api/users/*/information/photo").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/users/*/image", "/api/users/*/information/photo")
						.permitAll()

						.requestMatchers(HttpMethod.PUT, "/api/users/*/information/photo").hasRole("ADMIN")

						// Member Information details - ADMIN only
						.requestMatchers(HttpMethod.GET, "/api/users/*/information").hasRole("ADMIN")

						.requestMatchers(HttpMethod.PATCH, "/api/users/*/information").hasRole("ADMIN")

						// Existing user profile image
						.requestMatchers(HttpMethod.GET, "/api/users/*/image").permitAll()

						.requestMatchers(HttpMethod.PUT, "/api/users/*/image").hasRole("ADMIN")

						// Public GET endpoints
						.requestMatchers(HttpMethod.GET, "/api/events/**", "/api/meetings/**", "/api/users/**",
								"/api/contributions/**")
						.permitAll()

						// Business ideas: anyone can view, only logged-in users can
						// create/update/delete
						.requestMatchers(HttpMethod.GET, "/api/business-ideas", "/api/business-ideas/**").permitAll()

						// Programmes: any logged-in member can self-register
						.requestMatchers(HttpMethod.POST, "/api/programs/*/register").authenticated()

						// Programmes: only admin roles can create, edit, delete, assign, add expenses/media
						.requestMatchers(HttpMethod.POST, "/api/programs/*/feedback").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/programs/**", "/api/programs").hasAnyRole("ADMIN", "PRESIDENT", "SECRETARY", "TREASURER")
						.requestMatchers(HttpMethod.PUT, "/api/programs/**").hasAnyRole("ADMIN", "PRESIDENT", "SECRETARY", "TREASURER")
						.requestMatchers(HttpMethod.PATCH, "/api/programs/**").hasAnyRole("ADMIN", "PRESIDENT", "SECRETARY", "TREASURER")
						.requestMatchers(HttpMethod.DELETE, "/api/programs/**").hasAnyRole("ADMIN", "PRESIDENT", "SECRETARY", "TREASURER")

						// Notifications: only admin roles can broadcast
						.requestMatchers(HttpMethod.POST, "/api/notifications/broadcast").hasAnyRole("ADMIN", "PRESIDENT", "SECRETARY", "TREASURER")

						// Chatbot - public (widget sends no token)
						.requestMatchers("/api/chatbot/**").permitAll()

						// Admin-only operations
						.requestMatchers(HttpMethod.DELETE, "/api/events/**", "/api/meetings/**").hasRole("ADMIN")

						.requestMatchers("/api/admin/**").hasRole("ADMIN")

						// All remaining endpoints require authentication
						.anyRequest().authenticated())

				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {

		CorsConfiguration configuration = new CorsConfiguration();

		configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174"));

		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

		source.registerCorsConfiguration("/**", configuration);

		return source;
	}
}