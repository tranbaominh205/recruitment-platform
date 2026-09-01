package com.tbm.recruitment.employer.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CompanyResponse(
    UUID id,
    String name,
    String description,
    String website,
    String industry,
    String location,
    Instant createdAt,
    Instant updatedAt) {}
