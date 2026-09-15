package com.tbm.recruitment.notification.service;

import com.tbm.recruitment.notification.dto.response.NotificationCreatedSseEvent;
import com.tbm.recruitment.notification.enums.NotificationType;
import com.tbm.recruitment.notification.exception.AppException;
import com.tbm.recruitment.notification.exception.ErrorCode;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationSseService {

  static final long SSE_TIMEOUT_MILLIS = Duration.ofMinutes(30).toMillis();

  Map<UUID, Set<SseEmitter>> accountEmitters = new ConcurrentHashMap<>();
  Supplier<SseEmitter> emitterFactory;

  public NotificationSseService() {
    this(() -> new SseEmitter(SSE_TIMEOUT_MILLIS));
  }

  NotificationSseService(Supplier<SseEmitter> emitterFactory) {
    this.emitterFactory = emitterFactory;
  }

  public SseEmitter subscribe(String accountIdHeader, String accountRole) {
    UUID accountId = requireNotificationRecipient(accountIdHeader, accountRole);

    SseEmitter emitter = emitterFactory.get();

    emitter.onCompletion(() -> removeEmitter(accountId, emitter));
    emitter.onTimeout(() -> removeEmitter(accountId, emitter));
    emitter.onError(throwable -> removeEmitter(accountId, emitter));

    accountEmitters
        .computeIfAbsent(accountId, ignored -> ConcurrentHashMap.newKeySet())
        .add(emitter);

    try {
      emitter.send(SseEmitter.event().name("CONNECTED").data(Map.of("type", "CONNECTED")));
    } catch (IOException | IllegalStateException exception) {
      removeEmitter(accountId, emitter);
      try {
        emitter.complete();
      } catch (IllegalStateException ignored) {
        // no-op: the emitter is already closing or invalid.
      }
      throw new IllegalStateException(
          "Unable to establish SSE connection for account " + accountId, exception);
    }

    return emitter;
  }

  public void publishNotificationCreated(
      UUID recipientAccountId,
      UUID notificationId,
      NotificationType notificationType,
      UUID referenceId) {
    Set<SseEmitter> emitters = accountEmitters.get(recipientAccountId);
    if (emitters == null || emitters.isEmpty()) {
      return;
    }

    NotificationCreatedSseEvent payload =
        new NotificationCreatedSseEvent(
            "NOTIFICATION_CREATED", notificationId, notificationType, referenceId);

    for (SseEmitter emitter : Set.copyOf(emitters)) {
      try {
        emitter.send(SseEmitter.event().name("NOTIFICATION_CREATED").data(payload));
      } catch (IOException | IllegalStateException exception) {
        removeEmitter(recipientAccountId, emitter);
        try {
          emitter.complete();
        } catch (IllegalStateException ignored) {
          // no-op: the emitter is already closing or invalid.
        }
      }
    }
  }

  Set<SseEmitter> getAccountEmitters(UUID accountId) {
    return accountEmitters.getOrDefault(accountId, Set.of());
  }

  private void removeEmitter(UUID accountId, SseEmitter emitter) {
    Set<SseEmitter> emitters = accountEmitters.get(accountId);
    if (emitters == null) {
      return;
    }

    emitters.remove(emitter);
    if (emitters.isEmpty()) {
      accountEmitters.remove(accountId, emitters);
    }
  }

  private UUID requireNotificationRecipient(String accountIdHeader, String accountRole) {
    if (accountIdHeader == null || accountIdHeader.isBlank()) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    boolean supportedRole = "CANDIDATE".equals(accountRole) || "RECRUITER".equals(accountRole);
    if (!supportedRole) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }

    try {
      return UUID.fromString(accountIdHeader);
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
  }
}
