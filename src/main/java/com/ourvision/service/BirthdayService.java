package com.ourvision.service;

import java.util.List;

import com.ourvision.dto.BirthdayMemberDto;

public interface BirthdayService {

	void processTodayBirthdays();

	List<BirthdayMemberDto> getTodaysBirthdays();

}
