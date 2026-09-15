package com.tbm.recruitment.resume.dto.response;

import com.tbm.recruitment.resume.enums.ResumeStatus;
import java.time.Instant;
import java.util.UUID;

public record AdminResumeResponse(
    UUID id,
    UUID ownerAccountId,
    String displayName,
    String originalFileName,
    String contentType,
    long size,
    ResumeStatus status,
    Instant createdAt) {}
