package com.tbm.recruitment.notification.messaging;

import com.tbm.recruitment.notification.client.CandidateClient;
import com.tbm.recruitment.notification.dto.response.CandidateAccountResponse;
import com.tbm.recruitment.notification.enums.NotificationType;
import com.tbm.recruitment.notification.event.InterviewScheduledEvent;
import com.tbm.recruitment.notification.service.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class InterviewScheduledConsumer {

  CandidateClient candidateClient;
  NotificationService notificationService;

  @KafkaListener(
      topics = "${app.kafka.topics.interview-scheduled}",
      groupId = "notification-interview-scheduled",
      properties =
          "spring.json.value.default.type="
              + "com.tbm.recruitment.notification.event."
              + "InterviewScheduledEvent")
  public void consume(InterviewScheduledEvent event) {

    CandidateAccountResponse candidate = candidateClient.getCandidateAccount(event.candidateId());

    String title = "Interview scheduled";

    String message =
        "Your interview is scheduled for "
            + event.scheduledAt()
            + ". Location: "
            + event.location();

    notificationService.createNotification(
        event.eventId(),
        candidate.accountId(),
        NotificationType.INTERVIEW_SCHEDULED,
        title,
        message,
        event.applicationId());

    log.info(
        "Processed interview scheduled event. " + "eventId={}, applicationId={}",
        event.eventId(),
        event.applicationId());
  }
}
