package com.tbm.recruitment.notification.repository;

import com.tbm.recruitment.notification.entity.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository extends MongoRepository<Notification, UUID> {

  List<Notification> findAllByRecipientAccountIdOrderByCreatedAtDesc(UUID recipientAccountId);

  boolean existsBySourceEventId(UUID sourceEventId);
}
