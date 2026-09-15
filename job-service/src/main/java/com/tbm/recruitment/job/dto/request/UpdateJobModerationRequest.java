package com.tbm.recruitment.job.dto.request;

import com.tbm.recruitment.job.entity.JobModerationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateJobModerationRequest(
    @NotNull JobModerationStatus moderationStatus, String reason) {}
