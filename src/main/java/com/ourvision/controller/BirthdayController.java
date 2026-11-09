package com.ourvision.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ourvision.dto.BirthdayMemberDto;
import com.ourvision.service.BirthdayService;

@RestController
@RequestMapping("/api/birthdays")
public class BirthdayController {

	private final BirthdayService birthdayService;

	// Constructor initializes the final field
	public BirthdayController(BirthdayService birthdayService) {
		this.birthdayService = birthdayService;
	}

	@GetMapping("/today")
	public List<BirthdayMemberDto> getTodaysBirthdays() {
		return birthdayService.getTodaysBirthdays();
	}
}
