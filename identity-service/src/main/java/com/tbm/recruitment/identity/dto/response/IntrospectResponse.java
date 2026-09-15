package com.tbm.recruitment.identity.dto.response;

import java.util.List;

public record IntrospectResponse(
    boolean valid, String accountId, String email, String role, List<String> permissions) {}
