package com.ourvision.serviceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ourvision.entity.User;
import com.ourvision.repository.UserRepository;

// Runs once when app.migrate-images=true. Moves base64 photos in the database to files on disk.
@Component
@ConditionalOnProperty(name = "app.migrate-images", havingValue = "true")
public class ImageMigrationRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ImageMigrationRunner.class);

	private final UserRepository users;
	private final ImageStorageServiceImpl storage;

	public ImageMigrationRunner(UserRepository users, ImageStorageServiceImpl storage) {
		this.users = users;
		this.storage = storage;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {

		int moved = 0;

		for (User u : users.findAll()) {
			try {
				if (u.getImageUrl() != null && u.getImageUrl().startsWith("data:")) {
					u.setImageUrl(storage.storeDataUrl(u.getImageUrl(), "profile"));
					moved++;
				}
				if (u.getMemberPhotoUrl() != null && u.getMemberPhotoUrl().startsWith("data:")) {
					u.setMemberPhotoUrl(storage.storeDataUrl(u.getMemberPhotoUrl(), "member"));
					moved++;
				}
			} catch (RuntimeException ex) {
				log.error("Image migration failed for user {}", u.getId(), ex);
			}
		}

		log.info("Image migration finished: {} image(s) moved to files", moved);
	}
}