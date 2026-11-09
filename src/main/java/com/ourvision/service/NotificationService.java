package com.ourvision.service;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ourvision.dto.BroadcastRequest;
import com.ourvision.dto.NotificationResponse;
import com.ourvision.entity.Notification;
import com.ourvision.entity.User;
import com.ourvision.repository.NotificationRepository;
import com.ourvision.repository.ProgramParticipantRepository;
import com.ourvision.repository.UserRepository;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final UserRepository users;
    private final ProgramParticipantRepository participants;

    public NotificationService(NotificationRepository notifications, UserRepository users,
            ProgramParticipantRepository participants) {
        this.notifications = notifications;
        this.users = users;
        this.participants = participants;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> mine(String email) {
        return notifications.findRecent(currentUserId(email), PageRequest.of(0, 50)).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        return notifications.countUnread(currentUserId(email));
    }

    @Transactional
    public void markRead(String email, Long id) {
        Notification n = notifications.findByIdAndUserId(id, currentUserId(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        n.setRead(true);
    }

    @Transactional
    public void markAllRead(String email) {
        notifications.markAllRead(currentUserId(email));
    }

    /** Admin broadcast: all active members, or only one programme's participants. */
    @Transactional
    public int broadcast(BroadcastRequest req) {
        List<Long> ids = req.programId() != null
                ? participants.findByProgramId(req.programId()).stream().map(pp -> pp.getUser().getId()).toList()
                : activeMemberIds();
        notifyUsers(ids, req.title().trim(), req.message().trim(), "BROADCAST", req.programId());
        return (int) ids.stream().distinct().count();
    }

    /** Used by other services (e.g. ProgramService) to push a notification. */
    @Transactional
    public void notifyUsers(Collection<Long> userIds, String title, String message, String category, Long programId) {
        if (userIds == null || userIds.isEmpty()) return;
        List<User> targets = users.findAllById(userIds.stream().distinct().toList());
        List<Notification> rows = targets.stream().map(u -> {
            Notification n = new Notification();
            n.setUser(u);
            n.setTitle(title);
            n.setMessage(message);
            n.setCategory(category);
            n.setProgramId(programId);
            return n;
        }).toList();
        notifications.saveAll(rows);
    }

    @Transactional
    public void notifyAllActiveMembers(String title, String message, String category, Long programId) {
        notifyUsers(activeMemberIds(), title, message, category, programId);
    }

    // ---------- helpers ----------

    private List<Long> activeMemberIds() {
        return users.findAll().stream()
                .filter(u -> "ACTIVE".equals(u.getStatus()))
                .map(User::getId)
                .toList();
    }

    private Long currentUserId(String email) {
        return users.findByEmail(email).map(User::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getTitle(), n.getMessage(), n.getCategory(),
                n.getProgramId(), n.isRead(), n.getCreatedAt());
    }
}
