package com.tbm.recruitment.recruitment.dto.response;

import java.time.Instant;
import java.util.UUID;

public record InterviewResponse(
    UUID id,
    UUID applicationId,
    Instant scheduledAt,
    String location,
    String note,
    Instant createdAt) {}
