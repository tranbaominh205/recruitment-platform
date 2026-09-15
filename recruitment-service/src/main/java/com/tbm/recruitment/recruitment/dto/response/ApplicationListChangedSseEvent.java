package com.tbm.recruitment.recruitment.dto.response;

import com.tbm.recruitment.recruitment.enums.ApplicationListChange;
import java.util.UUID;

public record ApplicationListChangedSseEvent(
    String type, UUID jobId, UUID applicationId, ApplicationListChange change) {}
