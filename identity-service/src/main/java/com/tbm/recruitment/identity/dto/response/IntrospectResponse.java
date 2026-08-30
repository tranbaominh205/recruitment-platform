package com.tbm.recruitment.identity.dto.response;

public record IntrospectResponse(boolean valid, String accountId, String email, String role) {}
