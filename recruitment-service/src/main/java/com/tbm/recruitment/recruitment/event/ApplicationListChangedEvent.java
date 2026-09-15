package com.tbm.recruitment.recruitment.event;

import com.tbm.recruitment.recruitment.enums.ApplicationListChange;
import java.util.UUID;

public record ApplicationListChangedEvent(
    UUID jobId, UUID applicationId, ApplicationListChange change) {}
