package com.tbm.recruitment.job.dto.response;

import com.tbm.recruitment.job.entity.EducationLevel;
import com.tbm.recruitment.job.entity.JobStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobResponse(
    UUID id,
    UUID companyId,
    String title,
    String description,
    String requirements,
    List<String> requiredSkills,
    Integer minimumYearsExperience,
    EducationLevel requiredEducationLevel,
    String domain,
    String location,
    String employmentType,
    String workplaceType,
    BigDecimal salaryMin,
    BigDecimal salaryMax,
    JobStatus status,
    Instant createdAt,
    Instant updatedAt) {}
