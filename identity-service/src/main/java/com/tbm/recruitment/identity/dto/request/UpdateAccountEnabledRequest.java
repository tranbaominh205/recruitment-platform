package com.tbm.recruitment.identity.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateAccountEnabledRequest(@NotNull Boolean enabled) {}
