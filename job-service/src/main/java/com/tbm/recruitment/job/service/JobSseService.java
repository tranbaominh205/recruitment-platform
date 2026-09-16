package com.tbm.recruitment.job.service;

import com.tbm.recruitment.job.dto.response.JobListChangedSseEvent;
import com.tbm.recruitment.job.exception.AppException;
import com.tbm.recruitment.job.exception.ErrorCode;
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
public class JobSseService {

  static final long SSE_TIMEOUT_MILLIS = Duration.ofMinutes(30).toMillis();
  static final String EVENT_CONNECTED = "CONNECTED";
  static final String EVENT_MY_JOB_LIST_CHANGED = "MY_JOB_LIST_CHANGED";
  static final String EVENT_PUBLIC_JOB_LIST_CHANGED = "PUBLIC_JOB_LIST_CHANGED";

  Map<UUID, Set<SseEmitter>> candidateEmitters = new ConcurrentHashMap<>();
  Map<UUID, Set<SseEmitter>> recruiterEmitters = new ConcurrentHashMap<>();
  Supplier<SseEmitter> emitterFactory;

  public JobSseService() {
    this(() -> new SseEmitter(SSE_TIMEOUT_MILLIS));
  }

  JobSseService(Supplier<SseEmitter> emitterFactory) {
    this.emitterFactory = emitterFactory;
  }

  public SseEmitter subscribe(String accountIdHeader, String accountRole) {
    UUID accountId = requireAuthenticatedAccount(accountIdHeader);
    boolean candidate = "CANDIDATE".equals(accountRole);
    boolean recruiter = "RECRUITER".equals(accountRole);

    if (!candidate && !recruiter) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }

    SseEmitter emitter = emitterFactory.get();
    Map<UUID, Set<SseEmitter>> emitterRegistry = candidate ? candidateEmitters : recruiterEmitters;

    emitter.onCompletion(() -> removeEmitter(emitterRegistry, accountId, emitter));
    emitter.onTimeout(() -> removeEmitter(emitterRegistry, accountId, emitter));
    emitter.onError(throwable -> removeEmitter(emitterRegistry, accountId, emitter));

    emitterRegistry
        .computeIfAbsent(accountId, ignored -> ConcurrentHashMap.newKeySet())
        .add(emitter);

    try {
      emitter.send(SseEmitter.event().name(EVENT_CONNECTED).data(Map.of("type", EVENT_CONNECTED)));
    } catch (IOException | IllegalStateException exception) {
      removeEmitter(emitterRegistry, accountId, emitter);
      try {
        emitter.complete();
      } catch (IllegalStateException ignored) {
        // no-op: emitter is already closing or invalid.
      }
      throw new IllegalStateException(
          "Unable to establish job SSE connection for account " + accountId, exception);
    }

    return emitter;
  }

  public void publishMyJobListChanged(UUID recruiterAccountId, JobListChangedSseEvent payload) {
    publishToAccount(recruiterEmitters, recruiterAccountId, EVENT_MY_JOB_LIST_CHANGED, payload);
  }

  public void publishPublicJobListChanged(JobListChangedSseEvent payload) {
    for (Map.Entry<UUID, Set<SseEmitter>> entry : candidateEmitters.entrySet()) {
      publishToAccount(candidateEmitters, entry.getKey(), EVENT_PUBLIC_JOB_LIST_CHANGED, payload);
    }
  }

  Set<SseEmitter> getCandidateEmitters(UUID accountId) {
    return candidateEmitters.getOrDefault(accountId, Set.of());
  }

  Set<SseEmitter> getRecruiterEmitters(UUID accountId) {
    return recruiterEmitters.getOrDefault(accountId, Set.of());
  }

  private void publishToAccount(
      Map<UUID, Set<SseEmitter>> registry,
      UUID accountId,
      String eventName,
      JobListChangedSseEvent payload) {
    Set<SseEmitter> emitters = registry.get(accountId);
    if (emitters == null || emitters.isEmpty()) {
      return;
    }

    for (SseEmitter emitter : Set.copyOf(emitters)) {
      try {
        emitter.send(SseEmitter.event().name(eventName).data(payload));
      } catch (IOException | IllegalStateException exception) {
        removeEmitter(registry, accountId, emitter);
        try {
          emitter.complete();
        } catch (IllegalStateException ignored) {
          // no-op: emitter is already closing or invalid.
        }
      }
    }
  }

  private void removeEmitter(
      Map<UUID, Set<SseEmitter>> registry, UUID accountId, SseEmitter emitter) {
    Set<SseEmitter> emitters = registry.get(accountId);
    if (emitters == null) {
      return;
    }

    emitters.remove(emitter);
    if (emitters.isEmpty()) {
      registry.remove(accountId, emitters);
    }
  }

  private UUID requireAuthenticatedAccount(String accountIdHeader) {
    if (accountIdHeader == null || accountIdHeader.isBlank()) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    try {
      return UUID.fromString(accountIdHeader);
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
  }
}
