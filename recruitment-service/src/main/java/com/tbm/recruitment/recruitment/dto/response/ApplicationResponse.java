package com.tbm.recruitment.recruitment.dto.response;

import com.tbm.recruitment.recruitment.enums.ApplicationStatus;
import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
    UUID id,
    UUID candidateId,
    UUID jobId,
    UUID resumeId,
    ApplicationStatus status,
    Instant submittedAt) {}
