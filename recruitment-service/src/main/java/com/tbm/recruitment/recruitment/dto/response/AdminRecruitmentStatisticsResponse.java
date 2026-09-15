package com.tbm.recruitment.recruitment.dto.response;

public record AdminRecruitmentStatisticsResponse(
    long totalApplications,
    long submittedApplications,
    long screeningApplications,
    long interviewApplications,
    long offerApplications,
    long hiredApplications,
    long rejectedApplications,
    long withdrawnApplications,
    long totalInterviews) {}
