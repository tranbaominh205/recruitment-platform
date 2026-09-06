package com.tbm.recruitment.notification.messaging;

import com.tbm.recruitment.notification.client.CandidateClient;
import com.tbm.recruitment.notification.dto.response.CandidateAccountResponse;
import com.tbm.recruitment.notification.enums.NotificationType;
import com.tbm.recruitment.notification.event.ApplicationStatusChangedEvent;
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
public class ApplicationStatusChangedConsumer {

  CandidateClient candidateClient;
  NotificationService notificationService;

  @KafkaListener(topics = "${app.kafka.topics.application-status-changed}")
  public void consume(ApplicationStatusChangedEvent event) {

    CandidateAccountResponse candidate = candidateClient.getCandidateAccount(event.candidateId());

    String title = "Application status updated";

    String message =
        "Your application status changed from "
            + event.previousStatus()
            + " to "
            + event.newStatus();

    notificationService.createNotification(
        event.eventId(),
        candidate.accountId(),
        NotificationType.APPLICATION_STATUS_CHANGED,
        title,
        message,
        event.applicationId());

    log.info(
        "Processed application status event. eventId={}, applicationId={}",
        event.eventId(),
        event.applicationId());
  }
}
