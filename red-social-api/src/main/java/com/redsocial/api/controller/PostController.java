package com.redsocial.api.controller;

import com.redsocial.api.security.UserPrincipal;
import com.redsocial.api.service.CommentService;
import com.redsocial.api.service.PostService;
import com.redsocial.common.dto.CommentDto;
import com.redsocial.common.dto.CreateCommentRequest;
import com.redsocial.common.dto.CreatePostRequest;
import com.redsocial.common.dto.PageResponse;
import com.redsocial.common.dto.PostDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PostController {

    private final PostService postService;
    private final CommentService commentService;

    public PostController(PostService postService, CommentService commentService) {
        this.postService = postService;
        this.commentService = commentService;
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostDto create(@AuthenticationPrincipal UserPrincipal me,
                           @Valid @RequestBody CreatePostRequest request) {
        return postService.create(me.id(), request);
    }

    @GetMapping("/posts/{id}")
    public PostDto get(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return postService.get(id, me.id());
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        postService.delete(me.id(), id);
    }

    @PostMapping("/posts/{id}/like")
    public PostDto like(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return postService.like(me.id(), id);
    }

    @DeleteMapping("/posts/{id}/like")
    public PostDto unlike(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        return postService.unlike(me.id(), id);
    }

    @GetMapping("/posts/{id}/comments")
    public PageResponse<CommentDto> comments(@PathVariable Long id,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return commentService.list(id, page, size);
    }

    @PostMapping("/posts/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto addComment(@AuthenticationPrincipal UserPrincipal me,
                                 @PathVariable Long id,
                                  @Valid @RequestBody CreateCommentRequest request) {
        return commentService.add(me.id(), id, request);
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        commentService.delete(me.id(), id);
    }
}
