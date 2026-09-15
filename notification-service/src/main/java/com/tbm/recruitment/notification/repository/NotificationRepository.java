package com.tbm.recruitment.notification.repository;

import com.tbm.recruitment.notification.entity.Notification;
import com.tbm.recruitment.notification.enums.NotificationType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository extends MongoRepository<Notification, UUID> {

  List<Notification> findAllByRecipientAccountIdOrderByCreatedAtDesc(UUID recipientAccountId);

  List<Notification> findAllByRecipientAccountIdAndReadFalse(UUID recipientAccountId);

  long countByRecipientAccountIdAndRead(UUID recipientAccountId, boolean read);

  boolean existsBySourceEventId(UUID sourceEventId);

  java.util.Optional<Notification> findByIdAndRecipientAccountId(UUID id, UUID recipientAccountId);

  long countByRead(boolean read);

  long countByType(NotificationType type);
}
