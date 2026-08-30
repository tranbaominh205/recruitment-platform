package com.tbm.recruitment.identity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record IntrospectRequest(@NotBlank String token) {}
