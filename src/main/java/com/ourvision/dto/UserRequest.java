package com.ourvision.dto;

import com.ourvision.enm.BloodGroup;
import com.ourvision.enm.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Request payload for creating/updating a user profile. */
public record UserRequest(
        @NotBlank @Size(max = 150) String fullName,
        @Email @NotBlank @Size(max = 150) String email,
        @Size(max = 25) String phone,
        @NotBlank String role,
        LocalDate dateOfBirth,
        Gender gender,
        BloodGroup bloodGroup,
        @Size(max = 100) String designation,
        @Size(max = 200) String shortBio,
        String description,
        String skills,
        @Size(max = 500) String avatar,
        String imageUrl,
        @Size(max = 300) String linkedinUrl,
        @Size(max = 300) String githubUrl,
        String address,
        @Size(max = 255) String aadharNumber,
        @Size(max = 20) String status,
        Integer displayOrder
) {}
