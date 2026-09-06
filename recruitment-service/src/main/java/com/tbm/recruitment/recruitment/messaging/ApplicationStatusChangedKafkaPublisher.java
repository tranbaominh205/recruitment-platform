package com.tbm.recruitment.recruitment.messaging;

import com.tbm.recruitment.recruitment.event.ApplicationStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class ApplicationStatusChangedKafkaPublisher {

  private final KafkaTemplate<String, ApplicationStatusChangedEvent> kafkaTemplate;
  private final String topicName;

  public ApplicationStatusChangedKafkaPublisher(
      KafkaTemplate<String, ApplicationStatusChangedEvent> kafkaTemplate,
      @Value("${app.kafka.topics.application-status-changed}") String topicName) {

    this.kafkaTemplate = kafkaTemplate;
    this.topicName = topicName;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publish(ApplicationStatusChangedEvent event) {

    kafkaTemplate
        .send(topicName, event.applicationId().toString(), event)
        .whenComplete(
            (result, exception) -> {
              if (exception != null) {
                log.error(
                    "Failed to publish application status event. eventId={}, applicationId={}",
                    event.eventId(),
                    event.applicationId(),
                    exception);
                return;
              }

              log.info(
                  "Published application status event. eventId={}, applicationId={}, status={}",
                  event.eventId(),
                  event.applicationId(),
                  event.newStatus());
            });
  }
}
