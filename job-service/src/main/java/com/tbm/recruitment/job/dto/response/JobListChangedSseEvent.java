package com.tbm.recruitment.job.dto.response;

import com.tbm.recruitment.job.enums.JobListChange;
import java.util.UUID;

public record JobListChangedSseEvent(String type, UUID jobId, JobListChange change) {}
