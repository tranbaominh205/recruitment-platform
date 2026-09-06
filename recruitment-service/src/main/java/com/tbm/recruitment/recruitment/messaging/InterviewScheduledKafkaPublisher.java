package com.tbm.recruitment.recruitment.messaging;

import com.tbm.recruitment.recruitment.event.InterviewScheduledEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class InterviewScheduledKafkaPublisher {

  private final KafkaTemplate<String, InterviewScheduledEvent> kafkaTemplate;

  private final String topicName;

  public InterviewScheduledKafkaPublisher(
      KafkaTemplate<String, InterviewScheduledEvent> kafkaTemplate,
      @Value("${app.kafka.topics.interview-scheduled}") String topicName) {

    this.kafkaTemplate = kafkaTemplate;
    this.topicName = topicName;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publish(InterviewScheduledEvent event) {

    kafkaTemplate
        .send(topicName, event.applicationId().toString(), event)
        .whenComplete(
            (result, exception) -> {
              if (exception != null) {
                log.error(
                    "Failed to publish interview scheduled event. "
                        + "eventId={}, applicationId={}",
                    event.eventId(),
                    event.applicationId(),
                    exception);

                return;
              }

              log.info(
                  "Published interview scheduled event. " + "eventId={}, applicationId={}",
                  event.eventId(),
                  event.applicationId());
            });
  }
}
