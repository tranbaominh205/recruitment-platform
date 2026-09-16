package com.tbm.recruitment.job.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tbm.recruitment.job.dto.response.JobListChangedSseEvent;
import com.tbm.recruitment.job.enums.JobListChange;
import com.tbm.recruitment.job.event.JobListChangedEvent;
import com.tbm.recruitment.job.service.JobSseService;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

class JobListChangedSseListenerTest {

  @Test
  void listenerRoutesRecruiterAndCandidatePayloadsWhenPublicCatalogChanged() {
    JobSseService jobSseService = Mockito.mock(JobSseService.class);
    JobListChangedSseListener listener = new JobListChangedSseListener(jobSseService);
    JobListChangedEvent event =
        new JobListChangedEvent(
            UUID.randomUUID(), UUID.randomUUID(), JobListChange.PUBLISHED, true);

    listener.handleJobListChanged(event);

    verify(jobSseService)
        .publishMyJobListChanged(
            Mockito.eq(event.ownerAccountId()),
            argThat(
                payload ->
                    matchesPayload(payload, "MY_JOB_LIST_CHANGED", event.jobId(), event.change())));
    verify(jobSseService)
        .publishPublicJobListChanged(
            argThat(
                payload ->
                    matchesPayload(
                        payload, "PUBLIC_JOB_LIST_CHANGED", event.jobId(), event.change())));
  }

  @Test
  void listenerSkipsCandidatePublishWhenPublicCatalogUnchanged() {
    JobSseService jobSseService = Mockito.mock(JobSseService.class);
    JobListChangedSseListener listener = new JobListChangedSseListener(jobSseService);
    JobListChangedEvent event =
        new JobListChangedEvent(UUID.randomUUID(), UUID.randomUUID(), JobListChange.UPDATED, false);

    listener.handleJobListChanged(event);

    verify(jobSseService)
        .publishMyJobListChanged(Mockito.eq(event.ownerAccountId()), Mockito.any());
    verify(jobSseService, never()).publishPublicJobListChanged(Mockito.any());
  }

  @Test
  void listenerUsesAfterCommitWithFallbackExecution() throws NoSuchMethodException {
    Method method =
        JobListChangedSseListener.class.getDeclaredMethod(
            "handleJobListChanged", JobListChangedEvent.class);
    TransactionalEventListener annotation = method.getAnnotation(TransactionalEventListener.class);

    assertNotNull(annotation);
    assertEquals(TransactionPhase.AFTER_COMMIT, annotation.phase());
    assertTrue(annotation.fallbackExecution());
  }

  private boolean matchesPayload(
      JobListChangedSseEvent payload, String type, UUID jobId, JobListChange change) {
    return payload != null
        && type.equals(payload.type())
        && jobId.equals(payload.jobId())
        && change == payload.change();
  }
}
