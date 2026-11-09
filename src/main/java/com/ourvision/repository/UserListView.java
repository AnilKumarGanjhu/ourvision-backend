package com.ourvision.repository;

import java.time.OffsetDateTime;

public interface UserListView {
	Long getId();

	String getFullName();

	String getEmail();

	String getPhone();

	String getStatus();

	String getRole();

	OffsetDateTime getCreatedAt();

	OffsetDateTime getUpdatedAt();

	Integer getDisplayOrder();

	String getDesignation();

	String getShortBio();

	String getAvatar();

	String getAddress();

	Boolean getHasImage();

	Boolean getHasMemberPhoto();
}