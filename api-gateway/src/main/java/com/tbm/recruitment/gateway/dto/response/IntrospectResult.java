package com.tbm.recruitment.gateway.dto.response;

import java.util.List;

public record IntrospectResult(
    boolean valid, String accountId, String email, String role, List<String> permissions) {}
