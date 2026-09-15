package com.tbm.recruitment.employer.dto.request;

import com.tbm.recruitment.employer.entity.CompanyModerationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCompanyModerationRequest(
    @NotNull CompanyModerationStatus moderationStatus,
    @Size(max = 1000, message = "Reason must not exceed 1000 characters") String reason) {}
