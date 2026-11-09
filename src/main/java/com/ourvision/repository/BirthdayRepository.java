package com.ourvision.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ourvision.dto.BirthdayMemberDto;

@Repository
public class BirthdayRepository {

	private final JdbcTemplate jdbcTemplate;

	public BirthdayRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<BirthdayMemberDto> findBirthdays(LocalDate date) {
		String sql = """
				SELECT id, full_name
				FROM users
				WHERE date_of_birth IS NOT NULL
				  AND EXTRACT(MONTH FROM date_of_birth) = ?
				  AND EXTRACT(DAY FROM date_of_birth) = ?
				ORDER BY full_name
				""";

		return jdbcTemplate.query(sql,
				(rs, rowNum) -> new BirthdayMemberDto(rs.getLong("id"), rs.getString("full_name")),
				date.getMonthValue(), date.getDayOfMonth());
	}

	public int recordBirthdayIfAbsent(Long userId, int year) {
		String sql = """
				INSERT INTO birthday_wish_logs
				    (user_id, birthday_year, status)
				VALUES (?, ?, 'CREATED')
				ON CONFLICT (user_id, birthday_year)
				DO NOTHING
				""";

		return jdbcTemplate.update(sql, userId, year);
	}
}