package com.ourvision.service;

import java.util.List;

import com.ourvision.entity.BusinessIdea;

public interface BusinessIdeaService {

	BusinessIdea create(BusinessIdea idea);

	BusinessIdea update(Long id, BusinessIdea idea);

	void delete(Long id);

	BusinessIdea getById(Long id);

	List<BusinessIdea> getAll();

}
