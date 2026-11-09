package com.ourvision.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ourvision.dto.AttendanceBulkRequest;
import com.ourvision.entity.AttendanceRecord;
import com.ourvision.entity.Meeting;
import com.ourvision.entity.User;
import com.ourvision.repository.AttendanceRecordRepository;
import com.ourvision.repository.MeetingRepository;
import com.ourvision.repository.UserRepository;

@Service
public class AttendanceServiceImpl {
	private final AttendanceRecordRepository attendance;
	private final MeetingRepository meetings;
	private final UserRepository users;

	public AttendanceServiceImpl(AttendanceRecordRepository a, MeetingRepository m, UserRepository u) {
		attendance = a;
		meetings = m;
		users = u;
	}

	@Transactional
	public List<Map<String, Object>> bulk(AttendanceBulkRequest req, String email) {
		Meeting meeting = meetings.findById(req.meetingId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"));
		User actor = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		List<Map<String, Object>> result = new ArrayList<>();
		if (req.entries() == null) {
			return result;
		}
		for (var entry : req.entries()) {
			String status = entry.status().trim().toUpperCase(Locale.ROOT);
			if (!Set.of("PRESENT", "ABSENT", "EXCUSED").contains(status)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be PRESENT, ABSENT or EXCUSED");
			}
			User member = users.findById(entry.userId()).orElseThrow(
					() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + entry.userId()));
			AttendanceRecord row = attendance.findByMeetingIdAndUserId(meeting.getId(), member.getId())
					.orElseGet(AttendanceRecord::new);
			row.setMeeting(meeting);
			row.setUser(member);
			row.setStatus(status);
			row.setMarkedBy(actor);
			attendance.save(row);
			result.add(Map.of("id", row.getId(), "meetingId", meeting.getId(), "userId", member.getId(), "userName",
					member.getFullName(), "status", row.getStatus()));
		}
		return result;
	}

	public List<Map<String, Object>> byMeeting(Long meetingId) {
		if (!meetings.existsById(meetingId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found");
		}
		return attendance.findByMeetingId(meetingId).stream()
				.map(a -> Map.<String, Object>of("id", a.getId(), "meetingId", a.getMeeting().getId(), "meetingTitle",
						a.getMeeting().getTitle(), "meetingDate", a.getMeeting().getMeetingDate(), "userId",
						a.getUser().getId(), "userName", a.getUser().getFullName(), "status", a.getStatus()))
				.toList();
	}

	public List<Map<String, Object>> forUserEmail(String email) {
		User user = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		return forUser(user.getId());
	}

	public List<Map<String, Object>> forUser(Long userId) {
		return attendance.findByUserId(userId).stream()
				.sorted(Comparator.comparing(a -> a.getMeeting().getMeetingDate(), Comparator.reverseOrder()))
				.map(a -> Map.<String, Object>of("id", a.getId(), "meetingId", a.getMeeting().getId(), "meetingTitle",
						a.getMeeting().getTitle(), "meetingDate", a.getMeeting().getMeetingDate(), "status",
						a.getStatus()))
				.toList();
	}

	public Map<String, Object> memberDashboard(Long userId, LocalDate month) {
		LocalDate from = month.withDayOfMonth(1), to = from.plusMonths(1).minusDays(1);
		List<AttendanceRecord> rows = attendance.findByUserId(userId).stream().filter(
				a -> !a.getMeeting().getMeetingDate().isBefore(from) && !a.getMeeting().getMeetingDate().isAfter(to))
				.toList();
		long present = rows.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
		long excused = rows.stream().filter(a -> "EXCUSED".equals(a.getStatus())).count();
		long meetings = meetingsCount(from, to);
		long applicable = Math.max(0, meetings - excused);
		return Map.of("month", from.toString(), "presentMeetings", present, "applicableMeetings", applicable,
				"percentage", applicable == 0 ? 0.0 : Math.round(present * 10000.0 / applicable) / 100.0);
	}

	private long meetingsCount(LocalDate from, LocalDate to) {
		return meetings.countByMeetingDateBetween(from, to);
	}

	public Map<String, Object> dashboard(LocalDate month) {
		LocalDate from = month.withDayOfMonth(1), to = from.plusMonths(1).minusDays(1);
		List<AttendanceRecord> rows = attendance.findByMeetingMeetingDateBetween(from, to);
		long meetingCount = meetingsCount(from, to);
		long activeMembers = users.findAll().stream().filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus())).count();
		long present = rows.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
		long excused = rows.stream().filter(a -> "EXCUSED".equals(a.getStatus())).count();
		long applicable = Math.max(0, meetingCount * activeMembers - excused);
		return Map.of("month", from.toString(), "presentCount", present, "applicableCount", applicable, "percentage",
				applicable == 0 ? 0.0 : Math.round(present * 10000.0 / applicable) / 100.0);
	}
}