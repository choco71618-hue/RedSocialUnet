package com.redsocial.api.controller;

import com.redsocial.api.security.UserPrincipal;
import com.redsocial.api.service.ConnectionService;
import com.redsocial.api.service.ReviewService;
import com.redsocial.common.dto.ConnectionDto;
import com.redsocial.common.dto.CreateConnectionRequest;
import com.redsocial.common.dto.ReviewDto;
import com.redsocial.common.dto.ReviewRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

    private final ConnectionService connectionService;
    private final ReviewService reviewService;

    public ConnectionController(ConnectionService connectionService, ReviewService reviewService) {
        this.connectionService = connectionService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public List<ConnectionDto> mine(@AuthenticationPrincipal UserPrincipal me) {
        return connectionService.mine(me.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public ConnectionDto request(@AuthenticationPrincipal UserPrincipal me,
                                 @Valid @RequestBody CreateConnectionRequest request) {
        return connectionService.request(me.id(), request);
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('TUTOR')")
    public ConnectionDto accept(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return connectionService.accept(me.id(), id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('TUTOR')")
    public ConnectionDto reject(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return connectionService.reject(me.id(), id);
    }

    @PostMapping("/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public ReviewDto review(@AuthenticationPrincipal UserPrincipal me,
                            @PathVariable Long id,
                            @Valid @RequestBody ReviewRequest request) {
        return reviewService.create(me.id(), id, request);
    }
}
