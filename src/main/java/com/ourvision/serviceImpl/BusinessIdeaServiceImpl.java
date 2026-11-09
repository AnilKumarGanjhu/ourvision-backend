package com.ourvision.serviceImpl;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ourvision.entity.BusinessIdea;
import com.ourvision.repository.BusinessIdeaRepository;
import com.ourvision.service.BusinessIdeaService;


@Service
@Transactional
public class BusinessIdeaServiceImpl implements BusinessIdeaService{

	private final BusinessIdeaRepository repository;

    public BusinessIdeaServiceImpl(BusinessIdeaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BusinessIdea> getAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public BusinessIdea getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Business idea not found: " + id
                ));
    }

    public BusinessIdea create(BusinessIdea request) {
        BusinessIdea idea = new BusinessIdea();
        idea.setTitle(request.getTitle().trim());
        idea.setDescription(request.getDescription());

        return repository.save(idea);
    }

    public BusinessIdea update(Long id, BusinessIdea request) {
        BusinessIdea idea = getById(id);

        idea.setTitle(request.getTitle().trim());
        idea.setDescription(request.getDescription());

        return repository.save(idea);
    }

    public void delete(Long id) {
        BusinessIdea idea = getById(id);
        repository.delete(idea);
    }
}
