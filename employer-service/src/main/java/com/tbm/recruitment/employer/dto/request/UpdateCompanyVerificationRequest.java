package com.tbm.recruitment.employer.dto.request;

import com.tbm.recruitment.employer.entity.CompanyVerificationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCompanyVerificationRequest(
    @NotNull CompanyVerificationStatus verificationStatus,
    @Size(max = 1000, message = "Reason must not exceed 1000 characters") String reason) {}
