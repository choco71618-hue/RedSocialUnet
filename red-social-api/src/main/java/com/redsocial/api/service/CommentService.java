package com.redsocial.api.service;

import com.redsocial.api.domain.Comment;
import com.redsocial.api.domain.Post;
import com.redsocial.api.domain.User;
import com.redsocial.api.exception.BadRequestException;
import com.redsocial.api.exception.ForbiddenException;
import com.redsocial.api.exception.ResourceNotFoundException;
import com.redsocial.api.mapper.DtoMapper;
import com.redsocial.api.repository.CommentRepository;
import com.redsocial.api.repository.PostRepository;
import com.redsocial.common.dto.CommentDto;
import com.redsocial.common.dto.CreateCommentRequest;
import com.redsocial.common.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private static final int MAX_CONTENT_LENGTH = 1000;

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserService userService;
    private final DtoMapper mapper;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository,
                          PostRepository postRepository,
                          UserService userService,
                          DtoMapper mapper,
                          NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userService = userService;
        this.mapper = mapper;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentDto> list(Long postId, int page, int size) {
        if (!postRepository.existsById(postId)) {
            throw ResourceNotFoundException.of("Publicación", postId);
        }
        Page<Comment> comments = commentRepository.findByPost_IdOrderByCreatedAtAsc(postId, PageRequest.of(page, size));
        return PageResponse.of(
                comments.getContent().stream().map(mapper::toCommentDto).toList(),
                comments.getNumber(),
                comments.getSize(),
                comments.getTotalElements());
    }

    @Transactional
    public CommentDto add(Long currentUserId, Long postId, CreateCommentRequest request) {
        String content = request.content() == null ? null : request.content().trim();
        if (content == null || content.isEmpty()) {
            throw new BadRequestException("El comentario no puede estar vacío");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BadRequestException("El comentario no puede superar los " + MAX_CONTENT_LENGTH + " caracteres");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> ResourceNotFoundException.of("Publicación", postId));
        User author = userService.getEntity(currentUserId);
        Comment comment = commentRepository.save(Comment.builder()
                .post(post)
                .author(author)
                .content(content)
                .build());
        notificationService.notify(post.getAuthor(), author,
                com.redsocial.common.dto.NotificationType.COMMENT, postId,
                "@" + author.getUsername() + " comentó tu publicación");
        return mapper.toCommentDto(comment);
    }

    @Transactional
    public void delete(Long currentUserId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Comentario", commentId));
        if (!comment.getAuthor().getId().equals(currentUserId)
                && !comment.getPost().getAuthor().getId().equals(currentUserId)) {
            throw new ForbiddenException("No podés eliminar este comentario");
        }
        commentRepository.delete(comment);
    }
}
