package com.redsocial.api.service;

import com.redsocial.api.domain.Post;
import com.redsocial.api.domain.PostLike;
import com.redsocial.api.domain.User;
import com.redsocial.api.exception.BadRequestException;
import com.redsocial.api.exception.ForbiddenException;
import com.redsocial.api.exception.ResourceNotFoundException;
import com.redsocial.api.mapper.DtoMapper;
import com.redsocial.api.repository.CommentRepository;
import com.redsocial.api.repository.PostLikeRepository;
import com.redsocial.api.repository.PostRepository;
import com.redsocial.api.repository.UserRepository;
import com.redsocial.common.dto.CreatePostRequest;
import com.redsocial.common.dto.MediaType;
import com.redsocial.common.dto.PageResponse;
import com.redsocial.common.dto.PostDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

    private static final int MAX_CONTENT_LENGTH = 2000;

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final DtoMapper mapper;
    private final NotificationService notificationService;

    public PostService(PostRepository postRepository,
                       PostLikeRepository postLikeRepository,
                       CommentRepository commentRepository,
                       UserRepository userRepository,
                       UserService userService,
                       DtoMapper mapper,
                       NotificationService notificationService) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.mapper = mapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public PostDto create(Long currentUserId, CreatePostRequest request) {
        String content = request.content() == null ? null : request.content().trim();
        if (content == null || content.isEmpty()) {
            throw new BadRequestException("El contenido de la publicación no puede estar vacío");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BadRequestException("El contenido no puede superar los " + MAX_CONTENT_LENGTH + " caracteres");
        }
        User author = userService.getEntity(currentUserId);
        String mediaUrl = request.mediaUrl() == null ? null : request.mediaUrl().trim();
        if (mediaUrl != null && mediaUrl.length() > 500) {
            throw new BadRequestException("La URL del archivo no puede superar los 500 caracteres");
        }
        boolean hasMedia = mediaUrl != null && !mediaUrl.isEmpty();
        MediaType mediaType = hasMedia ? request.mediaType() : null;
        Post post = postRepository.save(Post.builder()
                .author(author)
                .content(content)
                .mediaUrl(hasMedia ? mediaUrl : null)
                .mediaType(mediaType)
                .build());
        return toDto(post, currentUserId);
    }

    @Transactional(readOnly = true)
    public PostDto get(Long postId, Long currentUserId) {
        return toDto(getEntity(postId), currentUserId);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostDto> feed(Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findFeedForUser(currentUserId, pageable);
        return toPage(posts, currentUserId);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostDto> byAuthor(String username, Long currentUserId, int page, int size) {
        User author = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
        Page<Post> posts = postRepository.findByAuthor_IdOrderByCreatedAtDesc(author.getId(), PageRequest.of(page, size));
        return toPage(posts, currentUserId);
    }

    @Transactional
    public PostDto like(Long currentUserId, Long postId) {
        Post post = getEntity(postId);
        boolean wasNew = !postLikeRepository.existsByPost_IdAndUser_Id(postId, currentUserId);
        if (wasNew) {
            postLikeRepository.save(PostLike.builder()
                    .post(post)
                    .user(userService.getEntity(currentUserId))
                    .build());
            User liker = userService.getEntity(currentUserId);
            notificationService.notify(post.getAuthor(), liker,
                    com.redsocial.common.dto.NotificationType.LIKE, postId,
                    "@" + liker.getUsername() + " dio like a tu publicación");
        }
        return toDto(post, currentUserId);
    }

    @Transactional
    public PostDto unlike(Long currentUserId, Long postId) {
        Post post = getEntity(postId);
        postLikeRepository.findByPost_IdAndUser_Id(postId, currentUserId)
                .ifPresent(postLikeRepository::delete);
        return toDto(post, currentUserId);
    }

    @Transactional
    public void delete(Long currentUserId, Long postId) {
        Post post = getEntity(postId);
        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new ForbiddenException("Solo el autor puede eliminar la publicación");
        }
        postLikeRepository.deleteByPost_Id(postId);
        commentRepository.deleteByPost_Id(postId);
        postRepository.delete(post);
    }

    private Post getEntity(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> ResourceNotFoundException.of("Publicación", postId));
    }

    private PostDto toDto(Post post, Long currentUserId) {
        long likes = postLikeRepository.countByPost_Id(post.getId());
        long comments = commentRepository.countByPost_Id(post.getId());
        boolean likedByMe = currentUserId != null
                && postLikeRepository.existsByPost_IdAndUser_Id(post.getId(), currentUserId);
        return mapper.toPostDto(post, likes, comments, likedByMe);
    }

    private PageResponse<PostDto> toPage(Page<Post> posts, Long currentUserId) {
        return PageResponse.of(
                posts.getContent().stream().map(post -> toDto(post, currentUserId)).toList(),
                posts.getNumber(),
                posts.getSize(),
                posts.getTotalElements());
    }
}
