package com.redsocial.api.controller;

import com.redsocial.api.security.UserPrincipal;
import com.redsocial.api.service.PostService;
import com.redsocial.common.dto.PageResponse;
import com.redsocial.common.dto.PostDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final PostService postService;

    public FeedController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public PageResponse<PostDto> feed(@AuthenticationPrincipal UserPrincipal me,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return postService.feed(me.id(), page, size);
    }
}
