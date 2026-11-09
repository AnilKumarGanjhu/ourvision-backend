package com.ourvision.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ourvision.dto.MemberInformationRequest;
import com.ourvision.dto.UserRequest;
import com.ourvision.entity.Role;
import com.ourvision.entity.User;
import com.ourvision.repository.RoleRepository;
import com.ourvision.repository.UserListView;
import com.ourvision.repository.UserRepository;
import com.ourvision.serviceImpl.FileCompressionServiceImpl;
import com.ourvision.serviceImpl.ImageStorageServiceImpl;
import com.ourvision.utill.UserUtill;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private static final Logger log = LoggerFactory.getLogger(UserController.class);

	private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

	private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/webp",
			"image/gif");

	private static final List<String> ALLOWED_MEMBER_PHOTO_TYPES = List.of("image/jpeg", "image/png", "image/webp");

	private final UserRepository users;
	private final RoleRepository roles;
	private final PasswordEncoder encoder;
	private final UserUtill userUtill;
	private final FileCompressionServiceImpl fileCompressionService;
	private final ImageStorageServiceImpl storage;

	public UserController(UserRepository users, RoleRepository roles, PasswordEncoder encoder, UserUtill userUtill,
			FileCompressionServiceImpl fileCompressionService, ImageStorageServiceImpl storage) {

		this.users = users;
		this.roles = roles;
		this.encoder = encoder;
		this.userUtill = userUtill;
		this.fileCompressionService = fileCompressionService;
		this.storage = storage;
	}

	private String maskAadhar(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		String digits = value.replaceAll("\\s", "");

		return digits.length() <= 4 ? "****" : "******" + digits.substring(digits.length() - 4);
	}

	private Map<String, Object> view(User u) {

		Map<String, Object> m = new LinkedHashMap<>();

		m.put("id", u.getId());
		m.put("fullName", u.getFullName());
		m.put("email", u.getEmail());
		m.put("phone", u.getPhone());
		m.put("dateOfBirth", u.getDateOfBirth());
		m.put("gender", u.getGender());
		m.put("bloodGroup", u.getBloodGroup());
		m.put("designation", u.getDesignation());
		m.put("shortBio", u.getShortBio());
		m.put("description", u.getDescription());
		m.put("skills", u.getSkills());
		m.put("avatar", u.getAvatar());
		m.put("linkedinUrl", u.getLinkedinUrl());
		m.put("githubUrl", u.getGithubUrl());
		m.put("address", u.getAddress());
		m.put("aadharNumber", maskAadhar(u.getAadharNumber()));
		m.put("status", u.getStatus());
		m.put("displayOrder", u.getDisplayOrder());
		m.put("role", u.getRole() == null ? null : u.getRole().getName());
		m.put("createdAt", u.getCreatedAt());
		m.put("updatedAt", u.getUpdatedAt());

		// Profile photo (image_url) - used by the "existing profile photo" card
		m.put("hasImage", u.getImageUrl() != null && !u.getImageUrl().isBlank());

		// Member photo (member_photo_url) - used by slider, table, detail page
		m.put("hasMemberPhoto", u.getMemberPhotoUrl() != null && !u.getMemberPhotoUrl().isBlank());

		return m;
	}

	private Map<String, Object> viewFromProjection(UserListView u) {

		Map<String, Object> m = new LinkedHashMap<>();

		m.put("id", u.getId());
		m.put("fullName", u.getFullName());
		m.put("email", u.getEmail());
		m.put("phone", u.getPhone());
		m.put("status", u.getStatus());
		m.put("role", u.getRole());
		m.put("designation", u.getDesignation());
		m.put("shortBio", u.getShortBio());
		m.put("avatar", u.getAvatar());
		m.put("address", u.getAddress());
		m.put("displayOrder", u.getDisplayOrder());
		m.put("createdAt", u.getCreatedAt());
		m.put("updatedAt", u.getUpdatedAt());
		m.put("hasImage", Boolean.TRUE.equals(u.getHasImage()));
		m.put("hasMemberPhoto", Boolean.TRUE.equals(u.getHasMemberPhoto()));

		return m;
	}

	@GetMapping
	public List<Map<String, Object>> all() {

		long start = System.nanoTime();

		List<UserListView> rows = users.findAllForList();

		long queryEnd = System.nanoTime();

		List<Map<String, Object>> result = rows.stream().map(this::viewFromProjection).toList();

		long end = System.nanoTime();

		log.debug("GET /api/users | DB: {} ms | Map: {} ms | Total: {} ms | Rows: {}", (queryEnd - start) / 1_000_000.0,
				(end - queryEnd) / 1_000_000.0, (end - start) / 1_000_000.0, rows.size());

		return result;
	}

	@GetMapping("/page")
	public Page<Map<String, Object>> page(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "25") int size, @RequestParam(required = false) String search,
			@RequestParam(required = false) String role, @RequestParam(required = false) String status) {

		int safePage = Math.max(0, page);
		int safeSize = Math.max(1, Math.min(size, 100));

		return users.findAllForList(normalize(search), normalizeRole(role), normalize(status),
				PageRequest.of(safePage, safeSize)).map(this::viewFromProjection);
	}

	@GetMapping("/{id}")
	public Map<String, Object> one(@PathVariable Long id) {
		return view(findUser(id));
	}

	// ---------------------------------------------------------
	// PROFILE IMAGE UPLOAD (image_url)
	// ---------------------------------------------------------

	@PutMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Transactional
	public Map<String, Object> uploadProfileImage(@PathVariable Long id, @RequestPart("file") MultipartFile file)
			throws IOException {

		User user = findUser(id);

		validateImage(file, ALLOWED_IMAGE_TYPES, "JPG, PNG, WEBP, or GIF");

		String contentType = file.getContentType().toLowerCase(Locale.ROOT);

		byte[] optimizedBytes = fileCompressionService.compress(file.getBytes(), file.getOriginalFilename(),
				contentType);

		String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(optimizedBytes);

		String thumbnail = userUtill.toThumbnailBase64(dataUrl, 400);

		if (thumbnail == null || thumbnail.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not process the selected image");
		}

		String oldPath = user.getImageUrl();

		user.setImageUrl(storage.storeDataUrl(thumbnail, "profile"));
		users.save(user);

		storage.deleteQuietly(oldPath);

		return Map.of("id", user.getId(), "hasImage", true, "message", "Profile image uploaded successfully");
	}

	@GetMapping("/{id}/image")
	public void getImage(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		User user = findUser(id);

		writeImageResponse(user.getImageUrl(), request, response);
	}

	// ---------------------------------------------------------
	// CREATE USER
	// ---------------------------------------------------------

	@PostMapping
	@Transactional
	public Map<String, Object> create(@Valid @RequestBody UserRequest req) {

		if (users.existsByEmail(req.email().trim())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
		}

		User user = new User();

		user.setPassword(encoder.encode(UUID.randomUUID().toString()));

		applyRequest(user, req);

		return view(users.save(user));
	}

	// ---------------------------------------------------------
	// UPDATE USER
	// ---------------------------------------------------------

	@PutMapping("/{id}")
	@Transactional
	public Map<String, Object> update(@PathVariable Long id, @Valid @RequestBody UserRequest req) {

		User user = findUser(id);

		if (users.existsByEmailAndIdNot(req.email().trim(), id)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
		}

		applyRequest(user, req);

		return view(users.save(user));
	}

	// ---------------------------------------------------------
	// MEMBER INFORMATION
	// ---------------------------------------------------------

	@GetMapping("/{id}/information")
	public Map<String, Object> getMemberInformation(@PathVariable Long id) {
		return informationView(findUser(id));
	}

	private Map<String, Object> informationView(User u) {

		Map<String, Object> m = new LinkedHashMap<>();

		m.put("id", u.getId());
		m.put("loanStatus", u.getLoanStatus());
		m.put("loanDuration", u.getLoanDuration());
		m.put("totalLoansTaken", u.getTotalLoansTaken());
		m.put("joiningDate", u.getJoiningDate());
		m.put("fatherName", u.getFatherName());
		m.put("motherName", u.getMotherName());
		m.put("maritalStatus", u.getMaritalStatus());
		m.put("nationality", u.getNationality());
		m.put("languagesKnown", u.getLanguagesKnown());
		m.put("emergencyContactPerson", u.getEmergencyContactPerson());

		m.put("nomineeName", u.getNomineeName());
		m.put("nomineeRelation", u.getNomineeRelation());

		m.put("hasMemberPhoto", u.getMemberPhotoUrl() != null && !u.getMemberPhotoUrl().isBlank());

		return m;
	}

	@PatchMapping("/{id}/information")
	@Transactional
	public Map<String, Object> updateMemberInformation(@PathVariable Long id,
			@RequestBody MemberInformationRequest req) {

		User u = findUser(id);

		if (req.loanStatus() != null && !req.loanStatus().isBlank()) {

			String loanStatus = req.loanStatus().trim().toUpperCase(Locale.ROOT);

			if (!List.of("DUE", "PENDING", "COMPLETED", "CLEAR").contains(loanStatus)) {

				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Loan status must be DUE, PENDING, COMPLETED, or CLEAR");
			}

			u.setLoanStatus(loanStatus);
		}

		if (req.totalLoansTaken() != null) {

			if (req.totalLoansTaken() < 0) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total loans taken cannot be negative");
			}

			u.setTotalLoansTaken(req.totalLoansTaken());
		}

		u.setLoanDuration(req.loanDuration());
		u.setJoiningDate(req.joiningDate());
		u.setFatherName(req.fatherName());
		u.setMotherName(req.motherName());
		u.setMaritalStatus(req.maritalStatus());
		u.setNationality(req.nationality());
		u.setLanguagesKnown(req.languagesKnown());
		u.setEmergencyContactPerson(req.emergencyContactPerson());

		u.setNomineeName(req.nomineeName());
		u.setNomineeRelation(req.nomineeRelation());

		return informationView(users.save(u));
	}

	// ---------------------------------------------------------
	// MEMBER PHOTO UPLOAD (member_photo_url)
	// ---------------------------------------------------------

	@PutMapping(value = "/{id}/information/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Transactional
	public Map<String, Object> uploadMemberPhoto(@PathVariable Long id, @RequestPart("file") MultipartFile file)
			throws IOException {

		User user = findUser(id);

		validateImage(file, ALLOWED_MEMBER_PHOTO_TYPES, "JPG, PNG, or WEBP");

		String contentType = file.getContentType().toLowerCase(Locale.ROOT);

		byte[] optimizedBytes = fileCompressionService.compress(file.getBytes(), file.getOriginalFilename(),
				contentType);

		String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(optimizedBytes);

		String thumbnail = userUtill.toThumbnailBase64(dataUrl, 400);

		if (thumbnail == null || thumbnail.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not process the selected photo");
		}

		String oldPath = user.getMemberPhotoUrl();

		user.setMemberPhotoUrl(storage.storeDataUrl(thumbnail, "member"));
		users.save(user);

		storage.deleteQuietly(oldPath);

		return Map.of("id", user.getId(), "hasMemberPhoto", true, "message", "Member photo uploaded successfully");
	}

	@GetMapping("/{id}/information/photo")
	public void getMemberPhoto(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		User user = findUser(id);

		writeImageResponse(user.getMemberPhotoUrl(), request, response);
	}

	// ---------------------------------------------------------
	// UPDATE USER STATUS
	// ---------------------------------------------------------

	@PatchMapping("/{id}/status")
	@Transactional
	public Map<String, Object> updateStatus(@PathVariable Long id, @RequestParam String status) {

		String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);

		if (!List.of("ACTIVE", "INACTIVE", "BLOCKED").contains(normalized)) {

			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be ACTIVE, INACTIVE, or BLOCKED");
		}

		User u = findUser(id);

		u.setStatus(normalized);

		return view(users.save(u));
	}

	// ---------------------------------------------------------
	// DELETE USER
	// ---------------------------------------------------------

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {

		User u = findUser(id);

		String profileImage = u.getImageUrl();
		String memberPhoto = u.getMemberPhotoUrl();

		users.delete(u);

		storage.deleteQuietly(profileImage);
		storage.deleteQuietly(memberPhoto);
	}

	// ---------------------------------------------------------
	// IMAGE VALIDATION
	// ---------------------------------------------------------

	private void validateImage(MultipartFile file, List<String> allowedTypes, String allowedTypeNames)
			throws IOException {

		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please select an image file");
		}

		if (file.getSize() > MAX_IMAGE_SIZE) {
			throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image must be 5 MB or smaller");
		}

		String contentType = file.getContentType();

		if (contentType == null || !allowedTypes.contains(contentType.toLowerCase(Locale.ROOT))) {

			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Unsupported image type. Use " + allowedTypeNames);
		}

		byte[] bytes = file.getBytes();

		if (!hasValidImageSignature(bytes, contentType.toLowerCase(Locale.ROOT))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image file");
		}
	}

	private boolean hasValidImageSignature(byte[] bytes, String contentType) {

		if (bytes == null || bytes.length < 12) {
			return false;
		}

		switch (contentType) {

		case "image/jpeg":
			return (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;

		case "image/png":
			return (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47;

		case "image/webp":
			return bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W'
					&& bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';

		case "image/gif":
			return bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8'
					&& (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a';

		default:
			return false;
		}
	}

	// ---------------------------------------------------------
	// IMAGE RESPONSE: files on disk, old base64 rows still supported
	// ---------------------------------------------------------

	private void writeImageResponse(String stored, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		if (stored == null || stored.isBlank()) {
			response.setStatus(HttpStatus.NO_CONTENT.value());
			return;
		}

		// Rows that still hold base64 text (before the migration).
		if (stored.startsWith("data:")) {
			writeLegacyBase64(stored, request, response);
			return;
		}

		Path file;
		try {
			file = storage.resolve(stored);
		} catch (IllegalArgumentException ex) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return;
		}

		if (!Files.isRegularFile(file)) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return;
		}

		long size = Files.size(file);
		String etag = "\"" + size + "-" + Files.getLastModifiedTime(file).toMillis() + "\"";

		response.setHeader("ETag", etag);
		response.setHeader("Cache-Control", "private, no-cache");
		response.setHeader("X-Content-Type-Options", "nosniff");

		if (etag.equals(request.getHeader("If-None-Match"))) {
			response.setStatus(HttpStatus.NOT_MODIFIED.value());
			return;
		}

		response.setContentType(ImageStorageServiceImpl.contentTypeFor(stored));
		response.setContentLengthLong(size);

		Files.copy(file, response.getOutputStream());
	}

	// Old behaviour for base64 rows. Remove after the migration is verified.
	private void writeLegacyBase64(String imageData, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		try {

			String[] parts = imageData.split(",", 2);

			if (parts.length != 2) {
				response.setStatus(HttpStatus.NO_CONTENT.value());
				return;
			}

			String header = parts[0].toLowerCase(Locale.ROOT);

			String contentType;

			if (header.contains("image/png")) {
				contentType = "image/png";
			} else if (header.contains("image/webp")) {
				contentType = "image/webp";
			} else if (header.contains("image/gif")) {
				contentType = "image/gif";
			} else if (header.contains("image/jpeg")) {
				contentType = "image/jpeg";
			} else {
				response.setStatus(HttpStatus.NO_CONTENT.value());
				return;
			}

			String etag = etagOf(imageData);

			response.setHeader("ETag", etag);
			response.setHeader("Cache-Control", "private, no-cache");
			response.setHeader("X-Content-Type-Options", "nosniff");

			if (etag.equals(request.getHeader("If-None-Match"))) {
				response.setStatus(HttpStatus.NOT_MODIFIED.value());
				return;
			}

			byte[] imageBytes = Base64.getDecoder().decode(parts[1]);

			response.setContentType(contentType);
			response.setContentLength(imageBytes.length);

			response.getOutputStream().write(imageBytes);

		} catch (IllegalArgumentException ex) {

			log.error("Invalid Base64 image data", ex);

			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
	}

	private String etagOf(String imageData) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(imageData.getBytes(StandardCharsets.UTF_8));
			return "\"" + HexFormat.of().formatHex(digest) + "\"";
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 not available", ex);
		}
	}

	// ---------------------------------------------------------
	// APPLY USER REQUEST
	// ---------------------------------------------------------

	private void applyRequest(User u, UserRequest req) {

		u.setFullName(req.fullName().trim());
		u.setEmail(req.email().trim().toLowerCase(Locale.ROOT));
		u.setPhone(req.phone());
		u.setDateOfBirth(req.dateOfBirth());
		u.setGender(req.gender());
		u.setBloodGroup(req.bloodGroup());
		u.setDesignation(req.designation());
		u.setShortBio(req.shortBio());
		u.setDescription(req.description());
		u.setSkills(req.skills());
		u.setAvatar(req.avatar());
		u.setLinkedinUrl(req.linkedinUrl());
		u.setGithubUrl(req.githubUrl());
		u.setAddress(req.address());
		u.setAadharNumber(req.aadharNumber());

		if (req.status() != null && !req.status().isBlank()) {

			String status = req.status().trim().toUpperCase(Locale.ROOT);

			if (!List.of("ACTIVE", "INACTIVE", "BLOCKED").contains(status)) {

				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user status");
			}

			u.setStatus(status);

		} else if (u.getStatus() == null || u.getStatus().isBlank()) {

			u.setStatus("ACTIVE");
		}

		if (req.displayOrder() != null) {
			u.setDisplayOrder(req.displayOrder());
		}

		String roleName = normalizeRole(req.role());

		Role role = roles.findByName(roleName)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role: " + roleName));

		u.setRole(role);

		if (req.imageUrl() != null) {

			String oldPath = u.getImageUrl();

			if (req.imageUrl().isBlank()) {
				u.setImageUrl(null);
			} else {
				String thumbnail = userUtill.toThumbnailBase64(req.imageUrl(), 400);
				u.setImageUrl(storage.storeDataUrl(thumbnail, "profile"));
			}

			storage.deleteQuietly(oldPath);
		}
	}

	// ---------------------------------------------------------
	// COMMON HELPERS
	// ---------------------------------------------------------

	private User findUser(Long id) {

		return users.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
	}

	private String normalize(String value) {

		return value == null || value.isBlank() ? null : value.trim();
	}

	private String normalizeRole(String role) {

		String value = normalize(role);

		if (value == null) {
			return null;
		}

		value = value.toUpperCase(Locale.ROOT);

		return value.startsWith("ROLE_") ? value : "ROLE_" + value;
	}
}