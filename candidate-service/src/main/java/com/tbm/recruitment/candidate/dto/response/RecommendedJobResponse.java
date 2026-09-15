package com.tbm.recruitment.candidate.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecommendedJobResponse(
    UUID id,
    UUID companyId,
    String title,
    String description,
    String location,
    String employmentType,
    String workplaceType,
    Instant createdAt,
    BigDecimal recommendationScore,
    List<String> recommendationReasons) {}
