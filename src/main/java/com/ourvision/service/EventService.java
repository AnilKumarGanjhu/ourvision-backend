package com.ourvision.service;

import org.springframework.web.multipart.MultipartFile;

public interface EventService {

    void deleteAttachmentByIndex(Long eventId, int index);

    StoredEventFile storeEventFile(MultipartFile file);

    record StoredEventFile(
            String url,
            String name,
            String type,
            long size
    ) {}
}