package com.redsocial.api.controller;

import com.redsocial.api.security.UserPrincipal;
import com.redsocial.api.service.NotificationService;
import com.redsocial.common.dto.NotificationDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationDto> list(@AuthenticationPrincipal UserPrincipal me) {
        return notificationService.list(me.id());
    }

    @GetMapping("/unread-count")
    public long unreadCount(@AuthenticationPrincipal UserPrincipal me) {
        return notificationService.unreadCount(me.id());
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        notificationService.markRead(me.id(), id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal UserPrincipal me) {
        notificationService.markAllRead(me.id());
    }
}