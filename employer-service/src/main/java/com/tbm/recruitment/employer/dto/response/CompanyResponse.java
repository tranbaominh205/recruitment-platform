package com.tbm.recruitment.employer.dto.response;

import com.tbm.recruitment.employer.entity.CompanyModerationStatus;
import java.time.Instant;
import java.util.UUID;

public record CompanyResponse(
    UUID id,
    String name,
    String description,
    String website,
    String industry,
    String location,
    CompanyModerationStatus moderationStatus,
    Instant createdAt,
    Instant updatedAt) {}
