package com.tbm.recruitment.job.dto.response;

public record AdminJobStatisticsResponse(
    long totalJobs, long draftJobs, long publishedJobs, long closedJobs) {}
