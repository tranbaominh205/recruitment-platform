package com.tbm.recruitment.recruitment.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.tbm.recruitment.recruitment.enums.ApplicationListChange;
import com.tbm.recruitment.recruitment.event.ApplicationListChangedEvent;
import com.tbm.recruitment.recruitment.service.ApplicationSseService;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

class ApplicationListChangedSseListenerTest {

  @Test
  void listenerDelegatesToApplicationSseService() {
    ApplicationSseService applicationSseService = Mockito.mock(ApplicationSseService.class);
    ApplicationListChangedSseListener listener =
        new ApplicationListChangedSseListener(applicationSseService);
    ApplicationListChangedEvent event =
        new ApplicationListChangedEvent(
            UUID.randomUUID(), UUID.randomUUID(), ApplicationListChange.SUBMITTED);

    listener.handleApplicationListChanged(event);

    verify(applicationSseService)
        .publishApplicationListChanged(event.jobId(), event.applicationId(), event.change());
  }

  @Test
  void listenerUsesAfterCommitWithFallbackExecution() throws NoSuchMethodException {
    Method method =
        ApplicationListChangedSseListener.class.getDeclaredMethod(
            "handleApplicationListChanged", ApplicationListChangedEvent.class);
    TransactionalEventListener annotation = method.getAnnotation(TransactionalEventListener.class);

    assertNotNull(annotation);
    assertEquals(TransactionPhase.AFTER_COMMIT, annotation.phase());
    assertTrue(annotation.fallbackExecution());
  }
}
