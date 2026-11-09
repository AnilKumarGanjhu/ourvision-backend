package com.ourvision.serviceImpl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ImageStorageServiceImpl {

	private final Path root;

	public ImageStorageServiceImpl(@Value("${app.upload-dir:uploads}") String uploadDir) {
		this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
		try {
			Files.createDirectories(root.resolve("members"));
		} catch (IOException ex) {
			throw new UncheckedIOException("Cannot create upload directory " + root, ex);
		}
	}

	// Saves bytes under a new random name, so a changed photo always gets a new
	// URL.
	// Returns the relative path that is stored in the database.
	public String save(String kind, byte[] bytes, String extension) {
		String relative = "members/" + kind + "-" + UUID.randomUUID() + "." + extension;
		try {
			Path target = resolve(relative);
			Files.createDirectories(target.getParent());
			Files.write(target, bytes);
			return relative;
		} catch (IOException ex) {
			throw new UncheckedIOException("Could not store image", ex);
		}
	}

	// Takes "data:image/...;base64,..." (from UserUtill) and writes it to disk.
	public String storeDataUrl(String dataUrl, String kind) {
		int comma = dataUrl.indexOf(',');
		if (!dataUrl.startsWith("data:") || comma < 0) {
			throw new IllegalArgumentException("Invalid image data");
		}

		String header = dataUrl.substring(0, comma).toLowerCase(Locale.ROOT);

		String extension;
		if (header.contains("image/png")) {
			extension = "png";
		} else if (header.contains("image/webp")) {
			extension = "webp";
		} else if (header.contains("image/gif")) {
			extension = "gif";
		} else if (header.contains("image/jpeg")) {
			extension = "jpg";
		} else {
			throw new IllegalArgumentException("Unsupported image type");
		}

		byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(comma + 1));
		return save(kind, bytes, extension);
	}

	// Blocks "../" tricks: the resolved path must stay inside the upload folder.
	public Path resolve(String relative) {
		Path p = root.resolve(relative).normalize();
		if (!p.startsWith(root)) {
			throw new IllegalArgumentException("Invalid image path");
		}
		return p;
	}

	// Old base64 values and empty values are ignored.
	public void deleteQuietly(String stored) {
		if (stored == null || stored.isBlank() || stored.startsWith("data:")) {
			return;
		}
		try {
			Files.deleteIfExists(resolve(stored));
		} catch (IOException | IllegalArgumentException ignored) {
			// Missing or already removed file is not an error for the user.
		}
	}

	public static String contentTypeFor(String relative) {
		String lower = relative.toLowerCase(Locale.ROOT);
		if (lower.endsWith(".png")) {
			return "image/png";
		}
		if (lower.endsWith(".webp")) {
			return "image/webp";
		}
		if (lower.endsWith(".gif")) {
			return "image/gif";
		}
		return "image/jpeg";
	}
}