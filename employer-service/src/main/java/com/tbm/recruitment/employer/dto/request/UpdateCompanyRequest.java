package com.tbm.recruitment.employer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
    @NotBlank(message = "Company name is required")
        @Size(max = 200, message = "Company name must not exceed 200 characters")
        String name,
    @Size(max = 2000, message = "Description must not exceed 2000 characters") String description,
    @Size(max = 255, message = "Website must not exceed 255 characters") String website,
    @Size(max = 150, message = "Industry must not exceed 150 characters") String industry,
    @Size(max = 200, message = "Location must not exceed 200 characters") String location) {}
