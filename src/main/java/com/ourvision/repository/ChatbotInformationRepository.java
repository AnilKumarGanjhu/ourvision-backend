
package com.ourvision.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ourvision.entity.ChatbotInformation;

public interface ChatbotInformationRepository extends JpaRepository<ChatbotInformation, Long> {

	List<ChatbotInformation> findByStatusIgnoreCase(String status);
}
