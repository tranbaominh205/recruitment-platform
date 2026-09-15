package com.tbm.recruitment.resume.dto.response;

public record AdminResumeStatisticsResponse(
    long totalResumes, long activeResumes, long archivedResumes) {}
