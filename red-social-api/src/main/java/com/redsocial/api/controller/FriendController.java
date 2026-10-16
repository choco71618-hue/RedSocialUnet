package com.redsocial.api.controller;

import com.redsocial.api.security.UserPrincipal;
import com.redsocial.api.service.FriendService;
import com.redsocial.common.dto.FriendRequestDto;
import com.redsocial.common.dto.UserDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping
    public List<UserDto> friends(@AuthenticationPrincipal UserPrincipal me) {
        return friendService.friends(me.id());
    }

    @GetMapping("/requests")
    public List<FriendRequestDto> incoming(@AuthenticationPrincipal UserPrincipal me) {
        return friendService.incoming(me.id());
    }

    @GetMapping("/requests/sent")
    public List<FriendRequestDto> outgoing(@AuthenticationPrincipal UserPrincipal me) {
        return friendService.outgoing(me.id());
    }

    @GetMapping("/requests/count")
    public long incomingCount(@AuthenticationPrincipal UserPrincipal me) {
        return friendService.incomingCount(me.id());
    }

    @GetMapping("/status/{username}")
    public String status(@AuthenticationPrincipal UserPrincipal me, @PathVariable String username) {
        return friendService.status(me.id(), username);
    }

    @GetMapping("/mutual/{username}")
    public long mutual(@AuthenticationPrincipal UserPrincipal me, @PathVariable String username) {
        return friendService.mutualCount(me.id(), username);
    }

    @PostMapping("/requests/{username}")
    public String request(@AuthenticationPrincipal UserPrincipal me, @PathVariable String username) {
        return friendService.request(me.id(), username);
    }

    @PostMapping("/requests/{id}/accept")
    public UserDto accept(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return friendService.accept(me.id(), id);
    }

    @PostMapping("/requests/{id}/reject")
    public void reject(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        friendService.reject(me.id(), id);
    }

    @DeleteMapping("/requests/{id}")
    public void cancel(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        friendService.cancel(me.id(), id);
    }

    @DeleteMapping("/{username}")
    public void remove(@AuthenticationPrincipal UserPrincipal me, @PathVariable String username) {
        friendService.remove(me.id(), username);
    }
}
