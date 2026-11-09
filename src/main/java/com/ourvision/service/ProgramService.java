package com.ourvision.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ourvision.dto.ProgramActionRequests.ExpenseRequest;
import com.ourvision.dto.ProgramActionRequests.ParticipantRequest;
import com.ourvision.dto.ProgramRequest;
import com.ourvision.dto.ProgramResponse;
import com.ourvision.entity.Program;
import com.ourvision.entity.ProgramExpense;
import com.ourvision.entity.ProgramMedia;
import com.ourvision.entity.ProgramParticipant;
import com.ourvision.entity.User;
import com.ourvision.repository.ProgramExpenseRepository;
import com.ourvision.repository.ProgramMediaRepository;
import com.ourvision.repository.ProgramParticipantRepository;
import com.ourvision.repository.ProgramRepository;
import com.ourvision.repository.UserRepository;

@Service
public class ProgramService {

    static final Set<String> CATEGORIES = Set.of("EDUCATION", "HEALTHCARE", "ENVIRONMENT", "SOCIAL_WELFARE");
    static final Set<String> STATUSES = Set.of("PLANNING", "ACTIVE", "COMPLETED", "ON_HOLD");
    static final Set<String> PARTICIPANT_ROLES = Set.of("VOLUNTEER", "PARTICIPANT");

    private final ProgramRepository programs;
    private final ProgramParticipantRepository participants;
    private final ProgramExpenseRepository expenses;
    private final ProgramMediaRepository media;
    private final UserRepository users;
    private final FileStorageService files;
    private final NotificationService notifications;

    public ProgramService(ProgramRepository programs, ProgramParticipantRepository participants,
            ProgramExpenseRepository expenses, ProgramMediaRepository media, UserRepository users,
            FileStorageService files, NotificationService notifications) {
        this.programs = programs;
        this.participants = participants;
        this.expenses = expenses;
        this.media = media;
        this.users = users;
        this.files = files;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<ProgramResponse> list() {
        return programs.findAll().stream().map(this::toResponse).toList();
    }

    /** Programme photos shown on the Gallery & Events page. */
    @Transactional(readOnly = true)
    public List<com.ourvision.dto.GalleryPhoto> galleryPhotos() {
        return media.findGalleryMedia().stream()
                .map(m -> new com.ourvision.dto.GalleryPhoto(m.getId(), m.getFileUrl(), m.getMediaType(),
                        m.getProgram().getId(), m.getProgram().getTitle(), m.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgramResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public ProgramResponse create(ProgramRequest req) {
        Program p = new Program();
        applyRequest(p, req, true);
        Program saved = programs.save(p);
        notifications.notifyAllActiveMembers("New programme: " + saved.getTitle(),
                "A new programme has been added. Check Our Programs for details.", "PROGRAM", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public ProgramResponse update(Long id, ProgramRequest req) {
        Program p = find(id);
        applyRequest(p, req, false);
        return toResponse(programs.save(p));
    }

    @Transactional
    public void delete(Long id) {
        Program p = find(id);
        if (p.getBannerUrl() != null) files.deleteFile(p.getBannerUrl());
        p.getMedia().forEach(m -> files.deleteFile(m.getFileUrl()));
        programs.delete(p);
    }

    @Transactional
    public ProgramResponse updateStatus(Long id, String status) {
        Program p = find(id);
        p.setStatus(validStatus(status));
        Program saved = programs.save(p);
        List<Long> ids = saved.getParticipants().stream().map(pp -> pp.getUser().getId()).toList();
        notifications.notifyUsers(ids, "Programme update: " + saved.getTitle(),
                "Status is now " + saved.getStatus().replace('_', ' ').toLowerCase() + ".", "PROGRAM", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public ProgramResponse assignParticipant(Long programId, ParticipantRequest req) {
        Program p = find(programId);
        User u = users.findById(req.userId())
                .orElseThrow(() -> bad(HttpStatus.NOT_FOUND, "Member not found"));
        if (participants.existsByProgramIdAndUserId(programId, u.getId())) {
            throw bad(HttpStatus.CONFLICT, "This member is already assigned to the programme");
        }
        ProgramParticipant link = new ProgramParticipant();
        link.setProgram(p);
        link.setUser(u);
        link.setRole(validRole(req.role()));
        participants.save(link);
        p.getParticipants().add(link);
        notifications.notifyUsers(List.of(u.getId()), "You were added to " + p.getTitle(),
                "Your role: " + link.getRole().toLowerCase() + ".", "PROGRAM", programId);
        return toResponse(p);
    }

    /** Member self-registration: the logged-in user joins as a PARTICIPANT. */
    @Transactional
    public ProgramResponse registerSelf(Long programId, String email) {
        User u = users.findByEmail(email)
                .orElseThrow(() -> bad(HttpStatus.UNAUTHORIZED, "User not found"));
        Program p = find(programId);
        if (participants.existsByProgramIdAndUserId(programId, u.getId())) {
            return toResponse(p);
        }
        ProgramParticipant link = new ProgramParticipant();
        link.setProgram(p);
        link.setUser(u);
        link.setRole("PARTICIPANT");
        participants.save(link);
        p.getParticipants().add(link);
        return toResponse(p);
    }

    @Transactional
    public ProgramResponse removeParticipant(Long programId, Long userId) {
        Program p = find(programId);
        ProgramParticipant link = participants.findByProgramIdAndUserId(programId, userId)
                .orElseThrow(() -> bad(HttpStatus.NOT_FOUND, "Assignment not found"));
        p.getParticipants().remove(link);
        participants.delete(link);
        return toResponse(p);
    }

    @Transactional
    public ProgramResponse addExpense(Long programId, ExpenseRequest req) {
        Program p = find(programId);
        if (req.amount().signum() < 0) throw bad(HttpStatus.BAD_REQUEST, "Amount cannot be negative");
        ProgramExpense e = new ProgramExpense();
        e.setProgram(p);
        e.setTitle(req.title().trim());
        e.setAmount(req.amount());
        e.setExpenseDate(req.expenseDate());
        expenses.save(e);
        p.getExpenses().add(e);
        return toResponse(p);
    }

    @Transactional
    public ProgramResponse deleteExpense(Long programId, Long expenseId) {
        Program p = find(programId);
        ProgramExpense e = expenses.findById(expenseId)
                .filter(x -> x.getProgram().getId().equals(programId))
                .orElseThrow(() -> bad(HttpStatus.NOT_FOUND, "Expense not found"));
        p.getExpenses().remove(e);
        expenses.delete(e);
        return toResponse(p);
    }

    @Transactional
    public ProgramResponse uploadBanner(Long programId, MultipartFile file) throws IOException {
        Program p = find(programId);
        String url = files.store(file);
        if (p.getBannerUrl() != null) files.deleteFile(p.getBannerUrl());
        p.setBannerUrl(url);
        return toResponse(programs.save(p));
    }

    @Transactional
    public ProgramResponse addMedia(Long programId, MultipartFile file, String type) throws IOException {
        Program p = find(programId);
        String mediaType = validMediaType(type, file);
        ProgramMedia m = new ProgramMedia();
        m.setProgram(p);
        m.setFileUrl(files.store(file));
        m.setOriginalName(file.getOriginalFilename());
        m.setMediaType(mediaType);
        media.save(m);
        p.getMedia().add(m);
        return toResponse(p);
    }

    @Transactional
    public ProgramResponse deleteMedia(Long programId, Long mediaId) {
        Program p = find(programId);
        ProgramMedia m = media.findById(mediaId)
                .filter(x -> x.getProgram().getId().equals(programId))
                .orElseThrow(() -> bad(HttpStatus.NOT_FOUND, "File not found"));
        files.deleteFile(m.getFileUrl());
        p.getMedia().remove(m);
        media.delete(m);
        return toResponse(p);
    }

    // ---------- helpers ----------

    private String validMediaType(String type, MultipartFile file) {
        String t = type == null ? "PHOTO" : type.toUpperCase();
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        switch (t) {
            case "VIDEO" -> {
                String ct = file.getContentType() == null ? "" : file.getContentType();
                if (!ct.startsWith("video/") && !name.matches(".*\\.(mp4|webm|mov|avi|mkv)$")) {
                    throw bad(HttpStatus.BAD_REQUEST, "Please upload a video file (mp4, webm, mov)");
                }
                return "VIDEO";
            }
            case "DOCUMENT" -> { return "DOCUMENT"; }
            case "PHOTO" -> { return "PHOTO"; }
            default -> throw bad(HttpStatus.BAD_REQUEST, "Invalid media type");
        }
    }

    private Program find(Long id) {
        return programs.findById(id).orElseThrow(() -> bad(HttpStatus.NOT_FOUND, "Programme not found"));
    }

    private void applyRequest(Program p, ProgramRequest req, boolean creating) {
        if (req.title() != null) p.setTitle(req.title().trim());
        if (req.category() != null) {
            String c = req.category().toUpperCase();
            if (!CATEGORIES.contains(c)) throw bad(HttpStatus.BAD_REQUEST, "Invalid category");
            p.setCategory(c);
        }
        p.setDescription(req.description());
        if (req.status() != null) p.setStatus(validStatus(req.status()));
        p.setStartDate(req.startDate());
        p.setEndDate(req.endDate());
        if (p.getStartDate() != null && p.getEndDate() != null && p.getEndDate().isBefore(p.getStartDate())) {
            throw bad(HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        }
        if (req.budget() != null) {
            if (req.budget().signum() < 0) throw bad(HttpStatus.BAD_REQUEST, "Budget cannot be negative");
            p.setBudget(req.budget());
        } else if (creating && p.getBudget() == null) {
            p.setBudget(BigDecimal.ZERO);
        }
        if (req.progress() != null) {
            if (req.progress() < 0 || req.progress() > 100) throw bad(HttpStatus.BAD_REQUEST, "Progress must be 0-100");
            p.setProgress(req.progress());
        }
        if (req.coordinatorId() == null) {
            p.setCoordinator(null);
        } else {
            User coordinator = users.findById(req.coordinatorId())
                    .orElseThrow(() -> bad(HttpStatus.BAD_REQUEST, "Coordinator not found"));
            p.setCoordinator(coordinator);
        }
    }

    private String validStatus(String s) {
        String v = s.toUpperCase();
        if (!STATUSES.contains(v)) throw bad(HttpStatus.BAD_REQUEST, "Invalid status");
        return v;
    }

    private String validRole(String r) {
        if (r == null || r.isBlank()) return "VOLUNTEER";
        String v = r.toUpperCase();
        if (!PARTICIPANT_ROLES.contains(v)) throw bad(HttpStatus.BAD_REQUEST, "Invalid role");
        return v;
    }

    private ProgramResponse toResponse(Program p) {
        BigDecimal spent = p.getExpenses().stream()
                .map(ProgramExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ProgramResponse.Participant> people = p.getParticipants().stream()
                .map(pp -> new ProgramResponse.Participant(
                        pp.getUser().getId(), pp.getUser().getFullName(),
                        pp.getUser().getEmail(), pp.getRole(), pp.getJoinedAt()))
                .toList();

        List<ProgramResponse.Expense> exps = p.getExpenses().stream()
                .map(e -> new ProgramResponse.Expense(e.getId(), e.getTitle(), e.getAmount(), e.getExpenseDate()))
                .toList();

        List<ProgramResponse.Media> med = p.getMedia().stream()
                .map(m -> new ProgramResponse.Media(m.getId(), m.getFileUrl(), m.getOriginalName(), m.getMediaType()))
                .toList();

        User c = p.getCoordinator();

        return new ProgramResponse(
                p.getId(), p.getTitle(), p.getCategory(), p.getDescription(), p.getStatus(),
                p.getStartDate(), p.getEndDate(), p.getBudget(), spent, p.getProgress(),
                c != null ? c.getId() : null,
                c != null ? c.getFullName() : null,
                p.getBannerUrl(), people.size(), people, exps, med,
                p.getCreatedAt(), p.getUpdatedAt());
    }

    private static ResponseStatusException bad(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
