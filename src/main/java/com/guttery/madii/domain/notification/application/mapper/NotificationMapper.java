package com.guttery.madii.domain.notification.application.mapper;

import com.guttery.madii.domain.notification.application.dto.NotificationInfo;
import com.guttery.madii.domain.notification.domain.model.Notification;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationMapper {
    public static NotificationInfo toNotificationInfo(Notification notification) {
        return new NotificationInfo(
                notification.getTitle(),
                notification.getContents(),
                notification.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );
    }
}
