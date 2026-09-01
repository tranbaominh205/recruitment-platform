package com.tbm.recruitment.candidate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UpdateCandidatePreferencesRequest(
    @NotNull(message = "Desired job titles are required")
        @Size(max = 10, message = "Desired job titles must not exceed 10 items")
        Set<
                @NotBlank(message = "Desired job title must not be blank")
                @Size(max = 100, message = "Desired job title must not exceed 100 characters")
                String>
            desiredJobTitles,
    @NotNull(message = "Preferred locations are required")
        @Size(max = 10, message = "Preferred locations must not exceed 10 items")
        Set<
                @NotBlank(message = "Preferred location must not be blank")
                @Size(max = 100, message = "Preferred location must not exceed 100 characters")
                String>
            preferredLocations,
    @NotNull(message = "Employment types are required")
        @Size(max = 10, message = "Employment types must not exceed 10 items")
        Set<
                @NotBlank(message = "Employment type must not be blank")
                @Size(max = 50, message = "Employment type must not exceed 50 characters") String>
            employmentTypes,
    @NotNull(message = "Workplace types are required")
        @Size(max = 10, message = "Workplace types must not exceed 10 items")
        Set<
                @NotBlank(message = "Workplace type must not be blank")
                @Size(max = 50, message = "Workplace type must not exceed 50 characters") String>
            workplaceTypes) {}
