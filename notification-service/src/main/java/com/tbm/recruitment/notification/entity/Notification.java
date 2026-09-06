package com.tbm.recruitment.notification.entity;

import com.tbm.recruitment.notification.enums.NotificationType;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

  @Id private UUID id;

  private UUID sourceEventId;

  private UUID recipientAccountId;

  private NotificationType type;

  private String title;

  private String message;

  private UUID referenceId;

  private boolean read;

  private Instant createdAt;
}
