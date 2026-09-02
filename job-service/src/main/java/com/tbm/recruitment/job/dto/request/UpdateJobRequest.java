package com.tbm.recruitment.job.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateJobRequest(
    @NotBlank(message = "Job title is required")
        @Size(max = 200, message = "Job title must not exceed 200 characters")
        String title,
    @NotBlank(message = "Job description is required")
        @Size(max = 5000, message = "Job description must not exceed 5000 characters")
        String description,
    @Size(max = 5000, message = "Requirements must not exceed 5000 characters") String requirements,
    @Size(max = 200, message = "Location must not exceed 200 characters") String location,
    @Size(max = 50, message = "Employment type must not exceed 50 characters")
        String employmentType,
    @Size(max = 50, message = "Workplace type must not exceed 50 characters") String workplaceType,
    @DecimalMin(value = "0.0", message = "Minimum salary must not be negative")
        BigDecimal salaryMin,
    @DecimalMin(value = "0.0", message = "Maximum salary must not be negative")
        BigDecimal salaryMax) {}
