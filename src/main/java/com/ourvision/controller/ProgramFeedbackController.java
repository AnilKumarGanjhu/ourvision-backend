package com.ourvision.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.ourvision.dto.FeedbackDtos.FeedbackList;
import com.ourvision.dto.FeedbackDtos.FeedbackRequest;
import com.ourvision.service.ProgramFeedbackService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/programs")
public class ProgramFeedbackController {

    private final ProgramFeedbackService service;

    public ProgramFeedbackController(ProgramFeedbackService service) {
        this.service = service;
    }

    @GetMapping("/{id}/feedback")
    public FeedbackList list(@PathVariable Long id) {
        return service.list(id);
    }

    /** Any registered participant (enforced in service and SecurityConfig). */
    @PostMapping("/{id}/feedback")
    public FeedbackList submit(@PathVariable Long id, @Valid @RequestBody FeedbackRequest req, Principal principal) {
        return service.submit(id, principal.getName(), req);
    }

    /** Admin roles only (enforced in SecurityConfig). */
    @DeleteMapping("/feedback/{feedbackId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long feedbackId) {
        service.delete(feedbackId);
    }
}
