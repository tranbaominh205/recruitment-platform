package com.tbm.recruitment.employer.dto.response;

import com.tbm.recruitment.employer.entity.CompanyModerationStatus;
import com.tbm.recruitment.employer.entity.CompanyVerificationStatus;
import java.time.Instant;
import java.util.UUID;

public record AdminCompanyResponse(
    UUID id,
    String name,
    String description,
    String website,
    String industry,
    String location,
    Instant createdAt,
    Instant updatedAt,
    CompanyModerationStatus moderationStatus,
    String moderationReason,
    Instant moderatedAt,
    CompanyVerificationStatus verificationStatus,
    String verificationReason,
    Instant verifiedAt) {}
