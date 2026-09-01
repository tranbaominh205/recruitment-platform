package com.tbm.recruitment.candidate.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCandidateProfileRequest(
    @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must not exceed 150 characters")
        String fullName,
    @Size(max = 30, message = "Phone must not exceed 30 characters") String phone,
    @Size(max = 200, message = "School must not exceed 200 characters") String school,
    @Size(max = 150, message = "Major must not exceed 150 characters") String major,
    @Min(value = 1900, message = "Graduation year is invalid")
        @Max(value = 2100, message = "Graduation year is invalid")
        Integer graduationYear,
    @Size(max = 150, message = "Location must not exceed 150 characters") String location) {}
