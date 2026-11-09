package com.ourvision.serviceImpl;

import com.ourvision.entity.Event;
import com.ourvision.repository.EventRepository;
import com.ourvision.service.EventService;
import com.ourvision.service.FileStorageService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final FileStorageService fileService;

    public EventServiceImpl(
            EventRepository eventRepository,
            FileStorageService fileService) {
        this.eventRepository = eventRepository;
        this.fileService = fileService;
    }

    @Override
    public StoredEventFile storeEventFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Uploaded file is empty.");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "File name is missing.");
        }

        String lowerName = originalName.toLowerCase(Locale.ROOT);
        String type = detectType(lowerName);

        try {
            String url = fileService.store(file);
            return new StoredEventFile(url, originalName, type, fileService.getFileSize(url));
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to store uploaded file: " + ex.getMessage(),
                    ex
            );
        }
    }

    private String detectType(String lowerName) {
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".png") || lowerName.endsWith(".webp")
                || lowerName.endsWith(".gif") || lowerName.endsWith(".bmp")
                || lowerName.endsWith(".avif")) {
            return "IMAGE";
        }
        if (lowerName.endsWith(".mp4") || lowerName.endsWith(".mov")
                || lowerName.endsWith(".webm") || lowerName.endsWith(".avi")
                || lowerName.endsWith(".mkv")) {
            return "VIDEO";
        }
        if (lowerName.endsWith(".mp3") || lowerName.endsWith(".wav")
                || lowerName.endsWith(".ogg") || lowerName.endsWith(".m4a")) {
            return "AUDIO";
        }
        return "DOCUMENT";
    }

    @Override
    public void deleteAttachmentByIndex(Long eventId, int index) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found with id: " + eventId));

        String fileUrlStr = event.getFileUrl();
        if (fileUrlStr == null || fileUrlStr.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No attachments found for this event.");
        }

        String[] urls = fileUrlStr.split(",");
        String[] types = event.getFileType() != null
                ? event.getFileType().split(",")
                : new String[0];
        String[] names = event.getFileName() != null
                ? event.getFileName().split(",")
                : new String[0];

        if (index < 0 || index >= urls.length) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid attachment index: " + index);
        }

        String urlToDelete = urls[index].trim();
        if (!urlToDelete.isBlank()) {
            fileService.deleteFile(urlToDelete);
        }

        List<String> urlList = new ArrayList<>(Arrays.asList(urls));
        List<String> typeList = new ArrayList<>(Arrays.asList(types));
        List<String> nameList = new ArrayList<>(Arrays.asList(names));

        urlList.remove(index);
        if (index < typeList.size()) typeList.remove(index);
        if (index < nameList.size()) nameList.remove(index);

        event.setFileUrl(urlList.isEmpty() ? null : String.join(",", urlList));
        event.setFileType(typeList.isEmpty() ? null : String.join(",", typeList));
        event.setFileName(nameList.isEmpty() ? null : String.join(",", nameList));

        eventRepository.save(event);
    }
}