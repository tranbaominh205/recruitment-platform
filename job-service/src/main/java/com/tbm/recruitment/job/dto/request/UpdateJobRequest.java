package com.tbm.recruitment.job.dto.request;

import com.tbm.recruitment.job.entity.EducationLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record UpdateJobRequest(
    @NotBlank(message = "Job title is required")
        @Size(max = 200, message = "Job title must not exceed 200 characters")
        String title,
    @NotBlank(message = "Job description is required")
        @Size(max = 5000, message = "Job description must not exceed 5000 characters")
        String description,
    @Size(max = 5000, message = "Requirements must not exceed 5000 characters") String requirements,
    @NotNull(message = "Required skills are required")
        @Size(min = 1, max = 50, message = "Required skills must contain between 1 and 50 items")
        List<
                @NotBlank(message = "Required skill must not be blank")
                @Size(max = 100, message = "Required skill must not exceed 100 characters") String>
            requiredSkills,
    @NotNull(message = "Minimum years of experience is required")
        @Min(value = 0, message = "Minimum years of experience must be at least 0")
        @Max(value = 50, message = "Minimum years of experience must not exceed 50")
        Integer minimumYearsExperience,
    @NotNull(message = "Required education level is required")
        EducationLevel requiredEducationLevel,
    @NotBlank(message = "Job domain is required")
        @Size(max = 100, message = "Job domain must not exceed 100 characters")
        String domain,
    @Size(max = 200, message = "Location must not exceed 200 characters") String location,
    @Size(max = 50, message = "Employment type must not exceed 50 characters")
        String employmentType,
    @Size(max = 50, message = "Workplace type must not exceed 50 characters") String workplaceType,
    @DecimalMin(value = "0.0", message = "Minimum salary must not be negative")
        BigDecimal salaryMin,
    @DecimalMin(value = "0.0", message = "Maximum salary must not be negative")
        BigDecimal salaryMax) {}
