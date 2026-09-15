package com.tbm.recruitment.notification.dto.response;

public record AdminNotificationStatisticsResponse(
    long totalNotifications,
    long readNotifications,
    long unreadNotifications,
    long applicationStatusChangedNotifications,
    long interviewScheduledNotifications) {}
