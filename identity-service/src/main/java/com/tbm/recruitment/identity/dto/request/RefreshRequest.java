package com.tbm.recruitment.identity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String token) {}
