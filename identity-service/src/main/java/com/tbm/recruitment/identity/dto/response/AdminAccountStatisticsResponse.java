package com.tbm.recruitment.identity.dto.response;

public record AdminAccountStatisticsResponse(
    long totalAccounts,
    long candidateAccounts,
    long recruiterAccounts,
    long adminAccounts,
    long enabledAccounts,
    long disabledAccounts) {}
