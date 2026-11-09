package com.ourvision.serviceImpl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ourvision.dto.BirthdayMemberDto;
import com.ourvision.repository.BirthdayRepository;
import com.ourvision.service.BirthdayService;

@Service
public class BirthdayServiceImpl implements BirthdayService {

	private static final Logger log = LoggerFactory.getLogger(BirthdayServiceImpl.class);

	private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

	private final BirthdayRepository birthdayRepository;

	public BirthdayServiceImpl(BirthdayRepository birthdayRepository) {
		this.birthdayRepository = birthdayRepository;
	}

	@Override
	public List<BirthdayMemberDto> getTodaysBirthdays() {
		LocalDate today = LocalDate.now(ZONE);
		return birthdayRepository.findBirthdays(today);
	}

	@Override
	@Transactional
	public void processTodayBirthdays() {
		LocalDate today = LocalDate.now(ZONE);

		List<BirthdayMemberDto> members = birthdayRepository.findBirthdays(today);

		int newRecords = 0;

		for (BirthdayMemberDto member : members) {
			newRecords += birthdayRepository.recordBirthdayIfAbsent(member.id(), today.getYear());
		}

		log.info("Birthday processing completed for {}. Today's birthdays: {}, new records: {}", today, members.size(),
				newRecords);
	}
}