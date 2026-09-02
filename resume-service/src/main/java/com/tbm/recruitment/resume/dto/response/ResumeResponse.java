package com.tbm.recruitment.resume.dto.response;

import com.tbm.recruitment.resume.enums.ResumeStatus;
import java.time.Instant;
import java.util.UUID;

public record ResumeResponse(
    UUID id,
    String displayName,
    String originalFileName,
    String contentType,
    long size,
    ResumeStatus status,
    Instant createdAt) {}
