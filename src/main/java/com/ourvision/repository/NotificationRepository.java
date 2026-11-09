package com.ourvision.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ourvision.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("select n from Notification n where n.user.id = :userId order by n.createdAt desc")
    List<Notification> findRecent(@Param("userId") Long userId, org.springframework.data.domain.Pageable page);

    @Query("select count(n) from Notification n where n.user.id = :userId and n.isRead = false")
    long countUnread(@Param("userId") Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("update Notification n set n.isRead = true where n.user.id = :userId and n.isRead = false")
    int markAllRead(@Param("userId") Long userId);
}
