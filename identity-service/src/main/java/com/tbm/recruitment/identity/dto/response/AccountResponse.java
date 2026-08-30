package com.tbm.recruitment.identity.dto.response;

import com.tbm.recruitment.identity.entity.Role;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
    UUID id, String email, Role role, boolean enabled, Instant createdAt) {}
