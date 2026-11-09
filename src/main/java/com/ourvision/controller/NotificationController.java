package com.ourvision.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.ourvision.dto.BroadcastRequest;
import com.ourvision.dto.NotificationResponse;
import com.ourvision.service.NotificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificationResponse> mine(Principal principal) {
        return service.mine(principal.getName());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(Principal principal) {
        return Map.of("count", service.unreadCount(principal.getName()));
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long id, Principal principal) {
        service.markRead(principal.getName(), id);
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(Principal principal) {
        service.markAllRead(principal.getName());
    }

    /** Admin roles only (enforced in SecurityConfig). */
    @PostMapping("/broadcast")
    public Map<String, Integer> broadcast(@Valid @RequestBody BroadcastRequest req) {
        return Map.of("sent", service.broadcast(req));
    }
}
