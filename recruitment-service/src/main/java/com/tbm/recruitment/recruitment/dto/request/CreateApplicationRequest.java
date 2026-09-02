package com.tbm.recruitment.recruitment.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateApplicationRequest(@NotNull UUID jobId, @NotNull UUID resumeId) {}
