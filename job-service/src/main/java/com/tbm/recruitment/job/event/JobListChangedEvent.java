package com.tbm.recruitment.job.event;

import com.tbm.recruitment.job.enums.JobListChange;
import java.util.UUID;

public record JobListChangedEvent(
    UUID jobId, UUID ownerAccountId, JobListChange change, boolean publicCatalogChanged) {}
