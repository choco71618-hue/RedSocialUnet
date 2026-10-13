package com.redsocial.api.service;

import com.redsocial.api.domain.Notification;
import com.redsocial.api.domain.User;
import com.redsocial.api.mapper.DtoMapper;
import com.redsocial.api.repository.NotificationRepository;
import com.redsocial.common.dto.NotificationDto;
import com.redsocial.common.dto.NotificationType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private static final int MAX_TEXT = 400;

    private final NotificationRepository notificationRepository;
    private final DtoMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository,
                               DtoMapper mapper,
                               SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.mapper = mapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void notify(User recipient, User actor, NotificationType type, Long targetId, String text) {
        if (recipient == null || actor == null || recipient.getId().equals(actor.getId())) {
            return;
        }
        String normalized = text == null ? null : text.trim();
        if (normalized == null || normalized.isEmpty()) {
            return;
        }
        if (normalized.length() > MAX_TEXT) {
            normalized = normalized.substring(0, MAX_TEXT);
        }
        Notification notification = notificationRepository.save(Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(type)
                .targetId(targetId)
                .text(normalized)
                .build());
        publish(notification);
    }

    public void publish(Notification notification) {
        NotificationDto dto = mapper.toNotificationDto(notification);
        messagingTemplate.convertAndSendToUser(
                notification.getRecipient().getUsername(), "/queue/notifications", dto);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> list(Long recipientId) {
        Pageable pageable = PageRequest.of(0, 30);
        return notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(recipientId, pageable).stream()
                .map(mapper::toNotificationDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long recipientId) {
        return notificationRepository.countByRecipient_IdAndReadFalse(recipientId);
    }

    @Transactional
    public void markRead(Long recipientId, Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getRecipient().getId().equals(recipientId)) {
                notification.setRead(true);
                notificationRepository.save(notification);
            }
        });
    }

    @Transactional
    public void markAllRead(Long recipientId) {
        notificationRepository.markAllAsRead(recipientId);
    }
}