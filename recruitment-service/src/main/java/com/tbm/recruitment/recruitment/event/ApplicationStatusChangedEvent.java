package com.tbm.recruitment.recruitment.event;

import java.time.Instant;
import java.util.UUID;

public record ApplicationStatusChangedEvent(
    UUID eventId,
    UUID applicationId,
    UUID candidateId,
    String previousStatus,
    String newStatus,
    Instant occurredAt) {}
