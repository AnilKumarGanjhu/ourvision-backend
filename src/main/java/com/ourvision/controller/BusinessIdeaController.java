package com.ourvision.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.ourvision.entity.BusinessIdea;
import com.ourvision.service.BusinessIdeaService;

import java.util.List;

@RestController
@RequestMapping("/api/business-ideas")
public class BusinessIdeaController {

    private final BusinessIdeaService service;

    public BusinessIdeaController(BusinessIdeaService service) {
        this.service = service;
    }

    public record BusinessIdeaRequest(
            @NotBlank(message = "Title is required")
            @Size(max = 200, message = "Title must not exceed 200 characters")
            String title,

            String description
    ) {
    }

    @GetMapping
    public List<BusinessIdea> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public BusinessIdea getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessIdea create(
            @Valid @RequestBody BusinessIdeaRequest request) {

        BusinessIdea idea = new BusinessIdea();
        idea.setTitle(request.title());
        idea.setDescription(request.description());

        return service.create(idea);
    }

    @PutMapping("/{id}")
    public BusinessIdea update(
            @PathVariable Long id,
            @Valid @RequestBody BusinessIdeaRequest request) {

        BusinessIdea idea = new BusinessIdea();
        idea.setTitle(request.title());
        idea.setDescription(request.description());

        return service.update(id, idea);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

