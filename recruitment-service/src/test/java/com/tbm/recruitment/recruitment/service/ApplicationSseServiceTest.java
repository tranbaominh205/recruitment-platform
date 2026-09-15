package com.tbm.recruitment.recruitment.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tbm.recruitment.recruitment.enums.ApplicationListChange;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class ApplicationSseServiceTest {

  @Test
  void multipleEmittersForSameJobReceiveApplicationListChanged() throws IOException {
    UUID jobId = UUID.randomUUID();
    SseEmitter first = mock(SseEmitter.class);
    SseEmitter second = mock(SseEmitter.class);
    AtomicInteger counter = new AtomicInteger();
    ApplicationSseService service =
        new ApplicationSseService(() -> counter.getAndIncrement() == 0 ? first : second);

    service.subscribe(jobId);
    service.subscribe(jobId);
    clearInvocations(first, second);

    service.publishApplicationListChanged(
        jobId, UUID.randomUUID(), ApplicationListChange.SUBMITTED);

    verify(first).send(any(SseEmitter.SseEventBuilder.class));
    verify(second).send(any(SseEmitter.SseEventBuilder.class));
  }

  @Test
  void jobAEmittersDoNotReceiveJobBEvents() throws IOException {
    UUID jobA = UUID.randomUUID();
    UUID jobB = UUID.randomUUID();
    SseEmitter emitterA1 = mock(SseEmitter.class);
    SseEmitter emitterA2 = mock(SseEmitter.class);
    SseEmitter emitterB1 = mock(SseEmitter.class);
    AtomicInteger counter = new AtomicInteger();
    ApplicationSseService service =
        new ApplicationSseService(
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

    service.subscribe(jobA);
    service.subscribe(jobA);
    service.subscribe(jobB);
    clearInvocations(emitterA1, emitterA2, emitterB1);

    service.publishApplicationListChanged(jobA, UUID.randomUUID(), ApplicationListChange.WITHDRAWN);

    verify(emitterA1).send(any(SseEmitter.SseEventBuilder.class));
    verify(emitterA2).send(any(SseEmitter.SseEventBuilder.class));
    verify(emitterB1, never()).send(any(SseEmitter.SseEventBuilder.class));
  }

  @Test
  void failedEmitterIsCleanedWhileHealthyEmitterStillReceivesEvents() throws IOException {
    UUID jobId = UUID.randomUUID();
    SseEmitter failingEmitter = mock(SseEmitter.class);
    SseEmitter healthyEmitter = mock(SseEmitter.class);
    AtomicInteger counter = new AtomicInteger();
    ApplicationSseService service =
        new ApplicationSseService(
            () -> counter.getAndIncrement() == 0 ? failingEmitter : healthyEmitter);

    service.subscribe(jobId);
    service.subscribe(jobId);
    clearInvocations(failingEmitter, healthyEmitter);

    doThrow(new IOException("broken emitter"))
        .when(failingEmitter)
        .send(any(SseEmitter.SseEventBuilder.class));

    assertDoesNotThrow(
        () ->
            service.publishApplicationListChanged(
                jobId, UUID.randomUUID(), ApplicationListChange.STATUS_CHANGED));

    verify(failingEmitter).send(any(SseEmitter.SseEventBuilder.class));
    verify(healthyEmitter).send(any(SseEmitter.SseEventBuilder.class));
    assertEquals(1, service.getJobEmitters(jobId).size());
    assertTrue(service.getJobEmitters(jobId).contains(healthyEmitter));
  }

  @Test
  void failedInitialConnectedSendRemovesEmitter() {
    ApplicationSseService service = new ApplicationSseService(FailingSseEmitter::new);
    UUID jobId = UUID.randomUUID();

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> service.subscribe(jobId));

    assertTrue(exception.getMessage().contains("Unable to establish SSE connection"));
    assertTrue(service.getJobEmitters(jobId).isEmpty());
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
