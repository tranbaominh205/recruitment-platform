package com.tbm.recruitment.notification.mapper;

import com.tbm.recruitment.notification.dto.response.NotificationResponse;
import com.tbm.recruitment.notification.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

  NotificationResponse toNotificationResponse(Notification notification);
}
