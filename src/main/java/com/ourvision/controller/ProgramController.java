package com.ourvision.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;

import com.ourvision.dto.ProgramActionRequests.ExpenseRequest;
import com.ourvision.dto.ProgramActionRequests.ParticipantRequest;
import com.ourvision.dto.ProgramActionRequests.StatusRequest;
import com.ourvision.dto.ProgramRequest;
import com.ourvision.dto.ProgramResponse;
import com.ourvision.service.ProgramService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/programs")
public class ProgramController {

    private final ProgramService service;

    public ProgramController(ProgramService service) {
        this.service = service;
    }

    /** Any logged-in user can view programmes. */
    @GetMapping
    public List<ProgramResponse> list() {
        return service.list();
    }

    /** Programme photos for the Gallery page (any logged-in user). */
    @GetMapping("/gallery-photos")
    public List<com.ourvision.dto.GalleryPhoto> galleryPhotos() {
        return service.galleryPhotos();
    }

    @GetMapping("/{id}")
    public ProgramResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    /** Admin roles only (enforced in SecurityConfig). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProgramResponse create(@Valid @RequestBody ProgramRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public ProgramResponse update(@PathVariable Long id, @Valid @RequestBody ProgramRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PatchMapping("/{id}/status")
    public ProgramResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        return service.updateStatus(id, req.status());
    }

    @PostMapping("/{id}/participants")
    public ProgramResponse assign(@PathVariable Long id, @Valid @RequestBody ParticipantRequest req) {
        return service.assignParticipant(id, req);
    }

    @DeleteMapping("/{id}/participants/{userId}")
    public ProgramResponse unassign(@PathVariable Long id, @PathVariable Long userId) {
        return service.removeParticipant(id, userId);
    }

    /** Any logged-in member can register themselves for a programme. */
    @PostMapping("/{id}/register")
    public ProgramResponse register(@PathVariable Long id, Principal principal) {
        return service.registerSelf(id, principal.getName());
    }

    @PostMapping("/{id}/expenses")
    public ProgramResponse addExpense(@PathVariable Long id, @Valid @RequestBody ExpenseRequest req) {
        return service.addExpense(id, req);
    }

    @DeleteMapping("/{id}/expenses/{expenseId}")
    public ProgramResponse deleteExpense(@PathVariable Long id, @PathVariable Long expenseId) {
        return service.deleteExpense(id, expenseId);
    }

    @PutMapping(value = "/{id}/banner", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProgramResponse banner(@PathVariable Long id, @RequestPart("file") MultipartFile file) throws IOException {
        return service.uploadBanner(id, file);
    }

    @PostMapping(value = "/{id}/media", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProgramResponse addMedia(@PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "PHOTO") String type) throws IOException {
        return service.addMedia(id, file, type);
    }

    @DeleteMapping("/{id}/media/{mediaId}")
    public ProgramResponse deleteMedia(@PathVariable Long id, @PathVariable Long mediaId) {
        return service.deleteMedia(id, mediaId);
    }
}
