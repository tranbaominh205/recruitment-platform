package com.tbm.recruitment.job.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tbm.recruitment.job.dto.response.JobListChangedSseEvent;
import com.tbm.recruitment.job.enums.JobListChange;
import com.tbm.recruitment.job.exception.AppException;
import com.tbm.recruitment.job.exception.ErrorCode;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class JobSseServiceTest {

  @Test
  void candidateCanSubscribeWithMultipleEmitters() {
    JobSseService service = new JobSseService(SseEmitter::new);
    UUID accountId = UUID.randomUUID();

    SseEmitter first = service.subscribe(accountId.toString(), "CANDIDATE");
    SseEmitter second = service.subscribe(accountId.toString(), "CANDIDATE");

    Set<SseEmitter> emitters = service.getCandidateEmitters(accountId);
    assertEquals(2, emitters.size());
    assertTrue(emitters.contains(first));
    assertTrue(emitters.contains(second));
  }

  @Test
  void recruiterCanSubscribeWithMultipleEmitters() {
    JobSseService service = new JobSseService(SseEmitter::new);
    UUID accountId = UUID.randomUUID();

    SseEmitter first = service.subscribe(accountId.toString(), "RECRUITER");
    SseEmitter second = service.subscribe(accountId.toString(), "RECRUITER");

    Set<SseEmitter> emitters = service.getRecruiterEmitters(accountId);
    assertEquals(2, emitters.size());
    assertTrue(emitters.contains(first));
    assertTrue(emitters.contains(second));
  }

  @Test
  void recruiterIsolationIsPreserved() throws IOException {
    UUID accountA = UUID.randomUUID();
    UUID accountB = UUID.randomUUID();
    SseEmitter emitterA1 = mock(SseEmitter.class);
    SseEmitter emitterA2 = mock(SseEmitter.class);
    SseEmitter emitterB1 = mock(SseEmitter.class);
    AtomicInteger counter = new AtomicInteger();
    JobSseService service =
        new JobSseService(
            () -> {
              int index = counter.getAndIncrement();
              if (index == 0) {
                return emitterA1;
              }
              if (index == 1) {
                return emitterA2;
              }
              return emitterB1;
            });

    service.subscribe(accountA.toString(), "RECRUITER");
    service.subscribe(accountA.toString(), "RECRUITER");
    service.subscribe(accountB.toString(), "RECRUITER");
    clearInvocations(emitterA1, emitterA2, emitterB1);

    service.publishMyJobListChanged(
        accountA,
        new JobListChangedSseEvent(
            "MY_JOB_LIST_CHANGED", UUID.randomUUID(), JobListChange.UPDATED));

    verify(emitterA1).send(any(SseEmitter.SseEventBuilder.class));
    verify(emitterA2).send(any(SseEmitter.SseEventBuilder.class));
    verify(emitterB1, never()).send(any(SseEmitter.SseEventBuilder.class));
  }

  @Test
  void publicEventReachesCandidatesOnly() throws IOException {
    UUID candidateId = UUID.randomUUID();
    UUID recruiterId = UUID.randomUUID();
    SseEmitter candidateEmitter = mock(SseEmitter.class);
    SseEmitter recruiterEmitter = mock(SseEmitter.class);
    AtomicInteger counter = new AtomicInteger();
    JobSseService service =
        new JobSseService(
            () -> counter.getAndIncrement() == 0 ? candidateEmitter : recruiterEmitter);

    service.subscribe(candidateId.toString(), "CANDIDATE");
    service.subscribe(recruiterId.toString(), "RECRUITER");
    clearInvocations(candidateEmitter, recruiterEmitter);

    service.publishPublicJobListChanged(
        new JobListChangedSseEvent(
            "PUBLIC_JOB_LIST_CHANGED", UUID.randomUUID(), JobListChange.PUBLISHED));

    verify(candidateEmitter).send(any(SseEmitter.SseEventBuilder.class));
    verify(recruiterEmitter, never()).send(any(SseEmitter.SseEventBuilder.class));
  }

  @Test
  void adminRoleIsForbidden() {
    JobSseService service = new JobSseService(SseEmitter::new);

    AppException exception =
        assertThrows(
            AppException.class, () -> service.subscribe(UUID.randomUUID().toString(), "ADMIN"));

    assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
  }

  @Test
  void missingOrInvalidAccountIdentityIsUnauthenticated() {
    JobSseService service = new JobSseService(SseEmitter::new);

    AppException missingAccount =
        assertThrows(AppException.class, () -> service.subscribe(null, "CANDIDATE"));
    assertEquals(ErrorCode.UNAUTHENTICATED, missingAccount.getErrorCode());

    AppException invalidAccount =
        assertThrows(AppException.class, () -> service.subscribe("not-a-uuid", "RECRUITER"));
    assertEquals(ErrorCode.UNAUTHENTICATED, invalidAccount.getErrorCode());
  }

  @Test
  void completionCallbackRemovesEmitterAndRegistryKey() {
    UUID accountId = UUID.randomUUID();
    SseEmitter emitter = mock(SseEmitter.class);
    AtomicReference<Runnable> completionCallback = new AtomicReference<>();
    doAnswer(
            invocation -> {
              completionCallback.set(invocation.getArgument(0));
              return null;
            })
        .when(emitter)
        .onCompletion(any(Runnable.class));

    JobSseService service = new JobSseService(() -> emitter);
    service.subscribe(accountId.toString(), "CANDIDATE");

    assertTrue(hasCandidateEmitterKey(service, accountId));
    assertNotNull(completionCallback.get());
    completionCallback.get().run();
    assertTrue(service.getCandidateEmitters(accountId).isEmpty());
    assertFalse(hasCandidateEmitterKey(service, accountId));
  }

  @Test
  void timeoutCallbackRemovesEmitterAndRegistryKey() {
    UUID accountId = UUID.randomUUID();
    SseEmitter emitter = mock(SseEmitter.class);
    AtomicReference<Runnable> timeoutCallback = new AtomicReference<>();
    doAnswer(
            invocation -> {
              timeoutCallback.set(invocation.getArgument(0));
              return null;
            })
        .when(emitter)
        .onTimeout(any(Runnable.class));

    JobSseService service = new JobSseService(() -> emitter);
    service.subscribe(accountId.toString(), "RECRUITER");

    assertTrue(hasRecruiterEmitterKey(service, accountId));
    assertNotNull(timeoutCallback.get());
    timeoutCallback.get().run();
    assertTrue(service.getRecruiterEmitters(accountId).isEmpty());
    assertFalse(hasRecruiterEmitterKey(service, accountId));
  }

  @Test
  void errorCallbackRemovesEmitterAndRegistryKey() {
    UUID accountId = UUID.randomUUID();
    SseEmitter emitter = mock(SseEmitter.class);
    AtomicReference<Consumer<Throwable>> errorCallback = new AtomicReference<>();
    doAnswer(
            invocation -> {
              errorCallback.set(invocation.getArgument(0));
              return null;
            })
        .when(emitter)
        .onError(any());

    JobSseService service = new JobSseService(() -> emitter);
    service.subscribe(accountId.toString(), "CANDIDATE");

    assertTrue(hasCandidateEmitterKey(service, accountId));
    assertNotNull(errorCallback.get());
    errorCallback.get().accept(new RuntimeException("simulated error"));
    assertTrue(service.getCandidateEmitters(accountId).isEmpty());
    assertFalse(hasCandidateEmitterKey(service, accountId));
  }

  @Test
  void failedPublishCleansSingleDeadEmitter() throws IOException {
    UUID accountId = UUID.randomUUID();
    SseEmitter failingEmitter = mock(SseEmitter.class);
    JobSseService service = new JobSseService(() -> failingEmitter);
    service.subscribe(accountId.toString(), "RECRUITER");
    clearInvocations(failingEmitter);

    doThrow(new IOException("broken emitter"))
        .when(failingEmitter)
        .send(any(SseEmitter.SseEventBuilder.class));

    service.publishMyJobListChanged(
        accountId,
        new JobListChangedSseEvent("MY_JOB_LIST_CHANGED", UUID.randomUUID(), JobListChange.CLOSED));

    assertTrue(service.getRecruiterEmitters(accountId).isEmpty());
    assertFalse(hasRecruiterEmitterKey(service, accountId));
  }

  @Test
  void failedInitialConnectedSendRemovesEmitter() {
    JobSseService service = new JobSseService(FailingSseEmitter::new);
    UUID accountId = UUID.randomUUID();

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> service.subscribe(accountId.toString(), "CANDIDATE"));

    assertTrue(exception.getMessage().contains("Unable to establish job SSE connection"));
    assertTrue(service.getCandidateEmitters(accountId).isEmpty());
    assertFalse(hasCandidateEmitterKey(service, accountId));
  }

  @Test
  void failedEmitterDoesNotBlockHealthyEmitter() throws IOException {
    UUID accountId = UUID.randomUUID();
    SseEmitter failingEmitter = mock(SseEmitter.class);
    SseEmitter healthyEmitter = mock(SseEmitter.class);
    AtomicInteger counter = new AtomicInteger();
    JobSseService service =
        new JobSseService(() -> counter.getAndIncrement() == 0 ? failingEmitter : healthyEmitter);

    service.subscribe(accountId.toString(), "CANDIDATE");
    service.subscribe(accountId.toString(), "CANDIDATE");
    clearInvocations(failingEmitter, healthyEmitter);

    doThrow(new IOException("broken emitter"))
        .when(failingEmitter)
        .send(any(SseEmitter.SseEventBuilder.class));

    assertDoesNotThrow(
        () ->
            service.publishPublicJobListChanged(
                new JobListChangedSseEvent(
                    "PUBLIC_JOB_LIST_CHANGED", UUID.randomUUID(), JobListChange.PUBLISHED)));

    verify(failingEmitter).send(any(SseEmitter.SseEventBuilder.class));
    verify(healthyEmitter).send(any(SseEmitter.SseEventBuilder.class));
    assertEquals(1, service.getCandidateEmitters(accountId).size());
    assertTrue(service.getCandidateEmitters(accountId).contains(healthyEmitter));
  }

  @SuppressWarnings("unchecked")
  private boolean hasCandidateEmitterKey(JobSseService service, UUID accountId) {
    try {
      Field emittersField = JobSseService.class.getDeclaredField("candidateEmitters");
      emittersField.setAccessible(true);
      Map<UUID, Set<SseEmitter>> emitters = (Map<UUID, Set<SseEmitter>>) emittersField.get(service);
      return emitters.containsKey(accountId);
    } catch (ReflectiveOperationException exception) {
      throw new RuntimeException(exception);
    }
  }

  @SuppressWarnings("unchecked")
  private boolean hasRecruiterEmitterKey(JobSseService service, UUID accountId) {
    try {
      Field emittersField = JobSseService.class.getDeclaredField("recruiterEmitters");
      emittersField.setAccessible(true);
      Map<UUID, Set<SseEmitter>> emitters = (Map<UUID, Set<SseEmitter>>) emittersField.get(service);
      return emitters.containsKey(accountId);
    } catch (ReflectiveOperationException exception) {
      throw new RuntimeException(exception);
    }
  }

  private static class FailingSseEmitter extends SseEmitter {
    FailingSseEmitter() {
      super(30_000L);
    }

    @Override
    public void send(Object object) throws IOException {
      throw new IOException("simulated initial send failure");
    }

    @Override
    public void send(SseEmitter.SseEventBuilder builder) throws IOException {
      throw new IOException("simulated initial send failure");
    }
  }
}
