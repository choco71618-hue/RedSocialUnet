package com.redsocial.api.controller;

import com.redsocial.api.security.UserPrincipal;
import com.redsocial.api.service.PostService;
import com.redsocial.api.service.UserService;
import com.redsocial.common.dto.PageResponse;
import com.redsocial.common.dto.PostDto;
import com.redsocial.common.dto.UpdateProfileRequest;
import com.redsocial.common.dto.UserDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final PostService postService;

    public UserController(UserService userService, PostService postService) {
        this.userService = userService;
        this.postService = postService;
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal UserPrincipal me) {
        return userService.getProfile(me.username(), me.id());
    }

    @PutMapping("/me")
    public UserDto updateMe(@AuthenticationPrincipal UserPrincipal me,
                            @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(me.id(), request);
    }

    @GetMapping("/search")
    public List<UserDto> search(@AuthenticationPrincipal UserPrincipal me,
                                @RequestParam("q") String query) {
        return userService.search(query, me.id());
    }

    @GetMapping("/{username}")
    public UserDto profile(@AuthenticationPrincipal UserPrincipal me,
                           @PathVariable String username) {
        return userService.getProfile(username, me.id());
    }

    @GetMapping("/{username}/posts")
    public PageResponse<PostDto> posts(@AuthenticationPrincipal UserPrincipal me,
                                       @PathVariable String username,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return postService.byAuthor(username, me.id(), page, size);
    }

    @GetMapping("/{username}/followers")
    public List<UserDto> followers(@AuthenticationPrincipal UserPrincipal me,
                                   @PathVariable String username) {
        return userService.followers(username, me.id());
    }

    @GetMapping("/{username}/following")
    public List<UserDto> following(@AuthenticationPrincipal UserPrincipal me,
                                   @PathVariable String username) {
        return userService.following(username, me.id());
    }

    @PostMapping("/{username}/follow")
    public UserDto follow(@AuthenticationPrincipal UserPrincipal me,
                          @PathVariable String username) {
        return userService.follow(me.id(), username);
    }

    @DeleteMapping("/{username}/follow")
    public UserDto unfollow(@AuthenticationPrincipal UserPrincipal me,
                            @PathVariable String username) {
        return userService.unfollow(me.id(), username);
    }
}
