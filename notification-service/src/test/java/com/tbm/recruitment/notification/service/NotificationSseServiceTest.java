package com.tbm.recruitment.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tbm.recruitment.notification.exception.AppException;
import com.tbm.recruitment.notification.exception.ErrorCode;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
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
