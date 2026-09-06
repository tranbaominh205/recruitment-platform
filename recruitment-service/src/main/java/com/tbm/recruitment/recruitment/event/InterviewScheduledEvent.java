package com.tbm.recruitment.recruitment.event;

import java.time.Instant;
import java.util.UUID;

public record InterviewScheduledEvent(
    UUID eventId,
    UUID applicationId,
    UUID candidateId,
    Instant scheduledAt,
    String location,
    Instant occurredAt) {}
