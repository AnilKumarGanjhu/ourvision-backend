
package com.ourvision.controller;

import com.ourvision.dto.EventRequest;
import com.ourvision.entity.Event;
import com.ourvision.repository.EventRepository;
import com.ourvision.service.EventService;
import com.ourvision.service.EventService.StoredEventFile;
import com.ourvision.service.FileStorageService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@CrossOrigin
public class EventController {

    private final EventRepository repo;
    private final FileStorageService fileService;
    private final EventService eventService;

    public EventController(
            EventRepository repo,
            FileStorageService fileService,
            EventService eventService) {
        this.repo = repo;
        this.fileService = fileService;
        this.eventService = eventService;
    }

    @GetMapping
    public List<Event> all() {
        return repo.findAll();
    }

    @GetMapping("/page")
    public Page<Event> page(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        return repo.findAll(PageRequest.of(safePage, safeSize, Sort.by("eventDate").descending()));
    }

    @PostMapping
    public Event create(@Valid @RequestBody EventRequest r) {
        Event e = new Event();
        apply(e, r);
        return repo.save(e);
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'PRESIDENT', 'SECRETARY', 'TREASURER')")
    public Event createWithFile(
            @RequestParam("title") String title,
            @RequestParam(value = "eventDate", required = false) String eventDate,
            @RequestParam(value = "venue", required = false) String venue,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "files", required = false)
            List<MultipartFile> files) {

        try {
            Event e = new Event();
            e.setTitle(title);

            if (eventDate != null && !eventDate.isEmpty()) {
                e.setEventDate(LocalDate.parse(eventDate));
            }

            e.setVenue(venue);
            e.setDescription(description);

            if (files != null && !files.isEmpty()) {
                List<String> fileUrls = new ArrayList<>();
                List<String> fileNames = new ArrayList<>();
                List<String> fileTypes = new ArrayList<>();
                long totalSize = 0;

                for (MultipartFile file : files) {
                    if (file == null || file.isEmpty()) {
                        continue;
                    }

                    StoredEventFile stored =
                            eventService.storeEventFile(file);

                    fileUrls.add(stored.url());
                    fileNames.add(stored.name());
                    fileTypes.add(stored.type());
                    totalSize += stored.size();
                }

                e.setFileUrl(
                        fileUrls.isEmpty() ? null : String.join(",", fileUrls)
                );
                e.setFileName(
                        fileNames.isEmpty() ? null : String.join(",", fileNames)
                );
                e.setFileType(
                        fileTypes.isEmpty() ? null : String.join(",", fileTypes)
                );
                e.setFileSize(totalSize);
            }

            return repo.save(e);

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "File upload failed: " + ex.getMessage(),
                    ex
            );
        }
    }

    @PutMapping("/{id}")
    public Event update(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest r) {

        Event e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"
                ));

        apply(e, r);
        return repo.save(e);
    }

    @PutMapping(
            value = "/{id}/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'PRESIDENT', 'SECRETARY', 'TREASURER')")
    public Event updateWithFile(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam(value = "eventDate", required = false) String eventDate,
            @RequestParam(value = "venue", required = false) String venue,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        try {
            Event e = repo.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Event not found"));

            // 1. Update basic fields
            e.setTitle(title);
            if (eventDate != null && !eventDate.isEmpty()) {
                e.setEventDate(LocalDate.parse(eventDate));
            }
            e.setVenue(venue);
            e.setDescription(description);

            // 2. Handle Files (Append logic)
            if (files != null && !files.isEmpty()) {
                
                // Purane data ko string se lists me convert karein
                List<String> allUrls = new ArrayList<>();
                List<String> allNames = new ArrayList<>();
                List<String> allTypes = new ArrayList<>();
                long currentTotalSize = e.getFileSize() != null ? e.getFileSize() : 0L;

                if (e.getFileUrl() != null && !e.getFileUrl().isBlank()) {
                    allUrls.addAll(Arrays.asList(e.getFileUrl().split(",")));
                }
                if (e.getFileName() != null && !e.getFileName().isBlank()) {
                    allNames.addAll(Arrays.asList(e.getFileName().split(",")));
                }
                if (e.getFileType() != null && !e.getFileType().isBlank()) {
                    allTypes.addAll(Arrays.asList(e.getFileType().split(",")));
                }

                // Naye files ko server par save karein aur lists me add karein
                for (MultipartFile file : files) {
                    if (file == null || file.isEmpty()) continue;

                    StoredEventFile stored = eventService.storeEventFile(file);
                    
                    allUrls.add(stored.url());
                    allNames.add(stored.name());
                    allTypes.add(stored.type());
                    currentTotalSize += stored.size();
                }

                // Wapas database ke liye comma-separated string banayein
                e.setFileUrl(String.join(",", allUrls));
                e.setFileName(String.join(",", allNames));
                e.setFileType(String.join(",", allTypes));
                e.setFileSize(currentTotalSize);
            }

            return repo.save(e);

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "File update failed: " + ex.getMessage(),
                    ex
            );
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Event e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"
                ));

        if (e.getFileUrl() != null && !e.getFileUrl().isBlank()) {
            for (String url : e.getFileUrl().split(",")) {
                if (!url.isBlank()) {
                    fileService.deleteFile(url.trim());
                }
            }
        }

        repo.delete(e);
    }

    private void apply(Event e, EventRequest r) {
        e.setTitle(r.title());
        e.setEventDate(r.eventDate());
        e.setVenue(r.venue());
        e.setDescription(r.description());
        e.setBannerUrl(r.bannerUrl());
    }

    @DeleteMapping("/{eventId}/attachments/{index}")
    public ResponseEntity<?> deleteAttachment(
            @PathVariable Long eventId,
            @PathVariable int index) {

        try {
            eventService.deleteAttachmentByIndex(eventId, index);
            return ResponseEntity.ok("Attachment deleted successfully");

        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(ex.getReason());

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting attachment: " + ex.getMessage());
        }
    }
}