package com.tbm.recruitment.candidate.client.dto;

import java.time.Instant;
import java.util.UUID;

public record JobSearchJobResponse(
    UUID id,
    UUID companyId,
    String title,
    String description,
    String location,
    String employmentType,
    String workplaceType,
    String status,
    String moderationStatus,
    Instant createdAt) {}
