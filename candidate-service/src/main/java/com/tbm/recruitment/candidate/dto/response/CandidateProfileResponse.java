package com.tbm.recruitment.candidate.dto.response;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CandidateProfileResponse(
    UUID id,
    String fullName,
    String phone,
    String school,
    String major,
    Integer graduationYear,
    String location,
    Set<String> desiredJobTitles,
    Set<String> preferredLocations,
    Set<String> employmentTypes,
    Set<String> workplaceTypes,
    Instant createdAt,
    Instant updatedAt) {}
