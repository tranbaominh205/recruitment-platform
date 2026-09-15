package com.tbm.recruitment.notification.service;

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

import com.tbm.recruitment.notification.enums.NotificationType;
import com.tbm.recruitment.notification.exception.AppException;
import com.tbm.recruitment.notification.exception.ErrorCode;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NotificationSseServiceTest {

  @Test
  void candidateConnectionRegistersUniqueEmitter() {
    NotificationSseService service = new NotificationSseService(SseEmitter::new);
    UUID accountId = UUID.randomUUID();

    SseEmitter first = service.subscribe(accountId.toString(), "CANDIDATE");
    SseEmitter second = service.subscribe(accountId.toString(), "CANDIDATE");

    Set<SseEmitter> emitters = service.getAccountEmitters(accountId);
    assertEquals(2, emitters.size());
    assertTrue(emitters.contains(first));
    assertTrue(emitters.contains(second));
  }

  @Test
  void adminRoleIsRejected() {
    NotificationSseService service = new NotificationSseService(SseEmitter::new);

    AppException exception =
        assertThrows(
            AppException.class, () -> service.subscribe(UUID.randomUUID().toString(), "ADMIN"));

    assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
  }

  @Test
  void missingOrInvalidAccountIdentityFailsConsistently() {
    NotificationSseService service = new NotificationSseService(SseEmitter::new);

    AppException missingAccountIdException =
        assertThrows(AppException.class, () -> service.subscribe(null, "CANDIDATE"));
    assertEquals(ErrorCode.UNAUTHENTICATED, missingAccountIdException.getErrorCode());

    AppException invalidAccountIdException =
        assertThrows(AppException.class, () -> service.subscribe("not-a-uuid", "CANDIDATE"));
    assertEquals(ErrorCode.UNAUTHENTICATED, invalidAccountIdException.getErrorCode());
  }

  @Test
  void failedInitialSendRemovesEmitterFromRegistry() {
    NotificationSseService service = new NotificationSseService(FailingSseEmitter::new);
    UUID accountId = UUID.randomUUID();

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> service.subscribe(accountId.toString(), "RECRUITER"));

    assertTrue(exception.getMessage().contains("Unable to establish SSE connection"));
    assertTrue(service.getAccountEmitters(accountId).isEmpty());
  }

  @Test
  void publishNotificationCreatedDeliversToAllEmittersOfOneAccount() throws IOException {
    UUID accountId = UUID.randomUUID();
    SseEmitter first = mock(SseEmitter.class);
    SseEmitter second = mock(SseEmitter.class);
    AtomicInteger counter = new AtomicInteger();
    NotificationSseService service =
        new NotificationSseService(() -> counter.getAndIncrement() == 0 ? first : second);

    service.subscribe(accountId.toString(), "CANDIDATE");
    service.subscribe(accountId.toString(), "CANDIDATE");
    clearInvocations(first, second);

    service.publishNotificationCreated(
        accountId,
        UUID.randomUUID(),
        NotificationType.APPLICATION_STATUS_CHANGED,
        UUID.randomUUID());

    verify(first).send(any(SseEmitter.SseEventBuilder.class));
    verify(second).send(any(SseEmitter.SseEventBuilder.class));
  }

  @Test
  void publishNotificationCreatedIsIsolatedPerAccount() throws IOException {
    UUID accountA = UUID.randomUUID();
    UUID accountB = UUID.randomUUID();
    SseEmitter emitterA1 = mock(SseEmitter.class);
    SseEmitter emitterA2 = mock(SseEmitter.class);
    SseEmitter emitterB1 = mock(SseEmitter.class);
    AtomicInteger counter = new AtomicInteger();
    NotificationSseService service =
        new NotificationSseService(
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
    service.subscribe(accountB.toString(), "CANDIDATE");
    clearInvocations(emitterA1, emitterA2, emitterB1);

    service.publishNotificationCreated(
        accountA, UUID.randomUUID(), NotificationType.INTERVIEW_SCHEDULED, UUID.randomUUID());

    verify(emitterA1).send(any(SseEmitter.SseEventBuilder.class));
    verify(emitterA2).send(any(SseEmitter.SseEventBuilder.class));
    verify(emitterB1, never()).send(any(SseEmitter.SseEventBuilder.class));
  }

  @Test
  void failedEmitterDoesNotBlockOthersAndIsCleanedUp() throws IOException {
    UUID accountId = UUID.randomUUID();
    SseEmitter failingEmitter = mock(SseEmitter.class);
    SseEmitter healthyEmitter = mock(SseEmitter.class);
    AtomicInteger counter = new AtomicInteger();
    NotificationSseService service =
        new NotificationSseService(
            () -> counter.getAndIncrement() == 0 ? failingEmitter : healthyEmitter);

    service.subscribe(accountId.toString(), "CANDIDATE");
    service.subscribe(accountId.toString(), "CANDIDATE");
    clearInvocations(failingEmitter, healthyEmitter);

    doThrow(new IOException("broken emitter"))
        .when(failingEmitter)
        .send(any(SseEmitter.SseEventBuilder.class));

    assertDoesNotThrow(
        () ->
            service.publishNotificationCreated(
                accountId, UUID.randomUUID(), NotificationType.INTERVIEW_SCHEDULED, null));

    verify(failingEmitter).send(any(SseEmitter.SseEventBuilder.class));
    verify(healthyEmitter).send(any(SseEmitter.SseEventBuilder.class));
    assertEquals(1, service.getAccountEmitters(accountId).size());
    assertTrue(service.getAccountEmitters(accountId).contains(healthyEmitter));
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
