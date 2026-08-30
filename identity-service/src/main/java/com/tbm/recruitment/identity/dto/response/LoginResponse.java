package com.tbm.recruitment.identity.dto.response;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {}
