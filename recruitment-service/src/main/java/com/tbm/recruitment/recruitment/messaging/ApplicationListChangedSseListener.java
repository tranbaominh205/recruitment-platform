package com.tbm.recruitment.recruitment.messaging;

import com.tbm.recruitment.recruitment.event.ApplicationListChangedEvent;
import com.tbm.recruitment.recruitment.service.ApplicationSseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApplicationListChangedSseListener {

  ApplicationSseService applicationSseService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handleApplicationListChanged(ApplicationListChangedEvent event) {
    applicationSseService.publishApplicationListChanged(
        event.jobId(), event.applicationId(), event.change());
  }
}
