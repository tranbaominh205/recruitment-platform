package com.tbm.recruitment.recruitment.service;

import com.tbm.recruitment.recruitment.dto.response.ApplicationListChangedSseEvent;
import com.tbm.recruitment.recruitment.enums.ApplicationListChange;
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
public class ApplicationSseService {

  static final long SSE_TIMEOUT_MILLIS = Duration.ofMinutes(30).toMillis();
  static final String EVENT_CONNECTED = "CONNECTED";
  static final String EVENT_APPLICATION_LIST_CHANGED = "APPLICATION_LIST_CHANGED";

  Map<UUID, Set<SseEmitter>> jobEmitters = new ConcurrentHashMap<>();
  Supplier<SseEmitter> emitterFactory;

  public ApplicationSseService() {
    this(() -> new SseEmitter(SSE_TIMEOUT_MILLIS));
  }

  ApplicationSseService(Supplier<SseEmitter> emitterFactory) {
    this.emitterFactory = emitterFactory;
  }

  public SseEmitter subscribe(UUID jobId) {
    SseEmitter emitter = emitterFactory.get();
    emitter.onCompletion(() -> removeEmitter(jobId, emitter));
    emitter.onTimeout(() -> removeEmitter(jobId, emitter));
    emitter.onError(throwable -> removeEmitter(jobId, emitter));

    jobEmitters.computeIfAbsent(jobId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);

    try {
      emitter.send(SseEmitter.event().name(EVENT_CONNECTED).data(Map.of("type", EVENT_CONNECTED)));
    } catch (IOException | IllegalStateException exception) {
      removeEmitter(jobId, emitter);
      try {
        emitter.complete();
      } catch (IllegalStateException ignored) {
        // no-op: the emitter is already closing or invalid.
      }
      throw new IllegalStateException(
          "Unable to establish SSE connection for job " + jobId, exception);
    }

    return emitter;
  }

  public void publishApplicationListChanged(
      UUID jobId, UUID applicationId, ApplicationListChange change) {
    Set<SseEmitter> emitters = jobEmitters.get(jobId);
    if (emitters == null || emitters.isEmpty()) {
      return;
    }

    ApplicationListChangedSseEvent payload =
        new ApplicationListChangedSseEvent(
            EVENT_APPLICATION_LIST_CHANGED, jobId, applicationId, change);

    for (SseEmitter emitter : Set.copyOf(emitters)) {
      try {
        emitter.send(SseEmitter.event().name(EVENT_APPLICATION_LIST_CHANGED).data(payload));
      } catch (IOException | IllegalStateException exception) {
        removeEmitter(jobId, emitter);
        try {
          emitter.complete();
        } catch (IllegalStateException ignored) {
          // no-op: the emitter is already closing or invalid.
        }
      }
    }
  }

  Set<SseEmitter> getJobEmitters(UUID jobId) {
    return jobEmitters.getOrDefault(jobId, Set.of());
  }

  private void removeEmitter(UUID jobId, SseEmitter emitter) {
    Set<SseEmitter> emitters = jobEmitters.get(jobId);
    if (emitters == null) {
      return;
    }

    emitters.remove(emitter);
    if (emitters.isEmpty()) {
      jobEmitters.remove(jobId, emitters);
    }
  }
}
