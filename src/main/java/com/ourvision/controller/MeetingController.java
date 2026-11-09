package com.ourvision.controller;

import com.ourvision.dto.MeetingRequest;
import com.ourvision.entity.Meeting;
import com.ourvision.repository.MeetingRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {
	private final MeetingRepository repo;

	public MeetingController(MeetingRepository repo) {
		this.repo = repo;
	}

	@GetMapping
	public List<Meeting> all() {
		return repo.findAll();
	}

	@GetMapping("/page")
	public Page<Meeting> page(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
		int safePage = Math.max(0, page);
		int safeSize = Math.max(1, Math.min(size, 100));
		return repo.findAll(PageRequest.of(safePage, safeSize, Sort.by("meetingDate").descending()));
	}

	@PostMapping
	public Meeting create(@Valid @RequestBody MeetingRequest r) {
		Meeting m = new Meeting();
		apply(m, r);
		return repo.save(m);
	}

	@PutMapping("/{id}")
	public Meeting update(@PathVariable Long id, @Valid @RequestBody MeetingRequest r) {
		Meeting m = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		apply(m, r);
		return repo.save(m);
	}

	private void apply(Meeting m, MeetingRequest r) {
		m.setTitle(r.title());
		m.setMeetingDate(r.meetingDate());
		m.setMeetingTime(r.meetingTime());
		m.setVenue(r.venue());
		m.setDescription(r.description());
	}
}
