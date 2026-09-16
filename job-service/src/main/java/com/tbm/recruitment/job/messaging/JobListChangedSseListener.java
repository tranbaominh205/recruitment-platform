package com.tbm.recruitment.job.messaging;

import com.tbm.recruitment.job.dto.response.JobListChangedSseEvent;
import com.tbm.recruitment.job.event.JobListChangedEvent;
import com.tbm.recruitment.job.service.JobSseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JobListChangedSseListener {

  JobSseService jobSseService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handleJobListChanged(JobListChangedEvent event) {
    JobListChangedSseEvent recruiterPayload =
        new JobListChangedSseEvent("MY_JOB_LIST_CHANGED", event.jobId(), event.change());
    jobSseService.publishMyJobListChanged(event.ownerAccountId(), recruiterPayload);

    if (event.publicCatalogChanged()) {
      JobListChangedSseEvent candidatePayload =
          new JobListChangedSseEvent("PUBLIC_JOB_LIST_CHANGED", event.jobId(), event.change());
      jobSseService.publishPublicJobListChanged(candidatePayload);
    }
  }
}
