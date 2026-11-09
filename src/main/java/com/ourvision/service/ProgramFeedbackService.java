package com.ourvision.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ourvision.dto.FeedbackDtos.FeedbackList;
import com.ourvision.dto.FeedbackDtos.FeedbackRequest;
import com.ourvision.dto.FeedbackDtos.FeedbackResponse;
import com.ourvision.entity.Program;
import com.ourvision.entity.ProgramFeedback;
import com.ourvision.entity.User;
import com.ourvision.repository.ProgramFeedbackRepository;
import com.ourvision.repository.ProgramParticipantRepository;
import com.ourvision.repository.ProgramRepository;
import com.ourvision.repository.UserRepository;

@Service
public class ProgramFeedbackService {

    private final ProgramFeedbackRepository feedback;
    private final ProgramRepository programs;
    private final ProgramParticipantRepository participants;
    private final UserRepository users;
    private final NotificationService notifications;

    public ProgramFeedbackService(ProgramFeedbackRepository feedback, ProgramRepository programs,
            ProgramParticipantRepository participants, UserRepository users, NotificationService notifications) {
        this.feedback = feedback;
        this.programs = programs;
        this.participants = participants;
        this.users = users;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public FeedbackList list(Long programId) {
        List<ProgramFeedback> rows = feedback.findByProgramIdOrderByCreatedAtDesc(programId);
        double avg = rows.stream().mapToInt(ProgramFeedback::getRating).average().orElse(0);
        return new FeedbackList(Math.round(avg * 10) / 10.0, rows.size(),
                rows.stream().map(this::toResponse).toList());
    }

    /** Only registered participants of an active or completed programme can review; re-submitting updates the review. */
    @Transactional
    public FeedbackList submit(Long programId, String email, FeedbackRequest req) {
        Program p = programs.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programme not found"));
        User u = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (!participants.existsByProgramIdAndUserId(programId, u.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only registered participants can give feedback");
        }
        if (!"ACTIVE".equals(p.getStatus()) && !"COMPLETED".equals(p.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Feedback opens once the programme is active or completed");
        }

        ProgramFeedback f = feedback.findByProgramIdAndUserId(programId, u.getId()).orElseGet(ProgramFeedback::new);
        f.setProgram(p);
        f.setUser(u);
        f.setRating(req.rating());
        String comment = req.comment() == null ? null : req.comment().trim();
        f.setComment(comment == null || comment.isEmpty() ? null : comment);
        feedback.save(f);

        User coordinator = p.getCoordinator();
        if (coordinator != null && !coordinator.getId().equals(u.getId())) {
            notifications.notifyUsers(List.of(coordinator.getId()),
                    "New feedback on " + p.getTitle(),
                    u.getFullName() + " rated it " + req.rating() + "/5.", "PROGRAM", programId);
        }
        return list(programId);
    }

    /** Admin moderation. */
    @Transactional
    public void delete(Long feedbackId) {
        ProgramFeedback f = feedback.findById(feedbackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        feedback.delete(f);
    }

    private FeedbackResponse toResponse(ProgramFeedback f) {
        return new FeedbackResponse(f.getId(), f.getUser().getId(), f.getUser().getFullName(),
                f.getRating(), f.getComment(), f.getCreatedAt());
    }
}
