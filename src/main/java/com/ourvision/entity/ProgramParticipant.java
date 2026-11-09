package com.ourvision.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "program_participants",
        uniqueConstraints = @UniqueConstraint(name = "uk_program_participant", columnNames = {"program_id", "user_id"}))
public class ProgramParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id")
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /** VOLUNTEER or PARTICIPANT */
    @Column(nullable = false, length = 20)
    private String role = "VOLUNTEER";

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Program getProgram() { return program; }
    public void setProgram(Program program) { this.program = program; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}
