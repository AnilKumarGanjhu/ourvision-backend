package com.ourvision.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ourvision.service.BirthdayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BirthdayScheduler {

	private static final Logger log = LoggerFactory.getLogger(BirthdayScheduler.class);

	private BirthdayService birthdayService;

	// Constructor injection
	public BirthdayScheduler(BirthdayService birthdayService) {
		this.birthdayService = birthdayService;
	}

	@Scheduled(cron = "0 5 0 * * *", zone = "Asia/Kolkata")
	public void processBirthdays() {
		log.info("Starting daily birthday processing");

		try {
			birthdayService.processTodayBirthdays();
		} catch (Exception ex) {
			log.error("Birthday processing failed", ex);
		}
	}
}