package com.ourvision.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ourvision.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

	@Query("""
			SELECT u.id AS id, u.fullName AS fullName, u.email AS email,
			       u.phone AS phone, u.status AS status, r.name AS role,
			       u.createdAt AS createdAt, u.updatedAt AS updatedAt,
			       u.displayOrder AS displayOrder, u.designation AS designation,
			       u.shortBio AS shortBio, u.avatar AS avatar,
			       u.address AS address,
			       CASE WHEN u.imageUrl IS NOT NULL AND u.imageUrl <> '' THEN true ELSE false END AS hasImage,
			       CASE WHEN u.memberPhotoUrl IS NOT NULL AND u.memberPhotoUrl <> '' THEN true ELSE false END AS hasMemberPhoto
			FROM User u JOIN u.role r
			ORDER BY u.displayOrder ASC NULLS LAST, u.id ASC
			""")
	List<UserListView> findAllForList();

	@Query(value = """
			SELECT u.id AS id, u.fullName AS fullName, u.email AS email,
			       u.phone AS phone, u.status AS status, r.name AS role,
			       u.createdAt AS createdAt, u.updatedAt AS updatedAt,
			       u.displayOrder AS displayOrder, u.designation AS designation,
			       u.shortBio AS shortBio, u.avatar AS avatar,
			       u.address AS address,
			       CASE WHEN u.imageUrl IS NOT NULL AND u.imageUrl <> '' THEN true ELSE false END AS hasImage,
			       CASE WHEN u.memberPhotoUrl IS NOT NULL AND u.memberPhotoUrl <> '' THEN true ELSE false END AS hasMemberPhoto
			FROM User u JOIN u.role r
			WHERE (:search IS NULL OR :search = '' OR
			       LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
			       LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
			       (u.phone IS NOT NULL AND LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%'))) OR
			       (u.designation IS NOT NULL AND LOWER(u.designation) LIKE LOWER(CONCAT('%', :search, '%'))))
			  AND (:role IS NULL OR :role = '' OR r.name = :role)
			  AND (:status IS NULL OR :status = '' OR u.status = :status)
			ORDER BY u.displayOrder ASC NULLS LAST, u.id ASC
			""", countQuery = """
			SELECT COUNT(u) FROM User u JOIN u.role r
			WHERE (:search IS NULL OR :search = '' OR
			       LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
			       LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
			       (u.phone IS NOT NULL AND LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%'))) OR
			       (u.designation IS NOT NULL AND LOWER(u.designation) LIKE LOWER(CONCAT('%', :search, '%'))))
			  AND (:role IS NULL OR :role = '' OR r.name = :role)
			  AND (:status IS NULL OR :status = '' OR u.status = :status)
			""")
	Page<UserListView> findAllForList(@Param("search") String search, @Param("role") String role,
			@Param("status") String status, Pageable pageable);
}