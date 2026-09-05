package com.tbm.recruitment.recruitment.dto.request;

import com.tbm.recruitment.recruitment.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateApplicationStatusRequest(@NotNull ApplicationStatus status) {}
