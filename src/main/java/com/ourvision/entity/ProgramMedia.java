package com.ourvision.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "program_media")
public class ProgramMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id")
    private Program program;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "original_name", length = 255)
    private String originalName;

    /** DOCUMENT or PHOTO */
    @Column(name = "media_type", nullable = false, length = 20)
    private String mediaType = "PHOTO";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Program getProgram() { return program; }
    public void setProgram(Program program) { this.program = program; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
