package com.tbm.recruitment.gateway.dto.response;

public record IntrospectResult(boolean valid, String accountId, String email, String role) {}
