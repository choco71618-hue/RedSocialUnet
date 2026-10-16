package com.redsocial.api.service;

import com.redsocial.api.domain.Connection;
import com.redsocial.api.domain.Review;
import com.redsocial.api.domain.User;
import com.redsocial.api.exception.BadRequestException;
import com.redsocial.api.exception.ConflictException;
import com.redsocial.api.exception.ForbiddenException;
import com.redsocial.api.exception.ResourceNotFoundException;
import com.redsocial.api.mapper.DtoMapper;
import com.redsocial.api.repository.ConnectionRepository;
import com.redsocial.api.repository.ReviewRepository;
import com.redsocial.api.repository.UserRepository;
import com.redsocial.common.dto.ConnectionStatus;
import com.redsocial.common.dto.NotificationType;
import com.redsocial.common.dto.ReviewDto;
import com.redsocial.common.dto.ReviewRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    private static final int MAX_COMMENT = 1000;

    private final ReviewRepository reviewRepository;
    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final DtoMapper mapper;

    public ReviewService(ReviewRepository reviewRepository,
                         ConnectionRepository connectionRepository,
                         UserRepository userRepository,
                         NotificationService notificationService,
                         DtoMapper mapper) {
        this.reviewRepository = reviewRepository;
        this.connectionRepository = connectionRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.mapper = mapper;
    }

    @Transactional
    public ReviewDto create(Long studentId, Long connectionId, ReviewRequest request) {
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Conexión", connectionId));
        if (!connection.getStudent().getId().equals(studentId)) {
            throw new ForbiddenException("Solo el estudiante de la conexión puede dejar una reseña");
        }
        if (connection.getStatus() != ConnectionStatus.ACCEPTED) {
            throw new BadRequestException("La conexión debe estar aceptada para poder reseñar");
        }
        if (reviewRepository.existsByConnection_Id(connectionId)) {
            throw new ConflictException("Ya dejaste una reseña para esta conexión");
        }

        Integer rating = request.rating();
        if (rating == null || rating < 1 || rating > 5) {
            throw new BadRequestException("La calificación debe estar entre 1 y 5");
        }
        if (request.reviewCode() != null && !request.reviewCode().isBlank()
                && !request.reviewCode().trim().equalsIgnoreCase(connection.getReviewCode())) {
            throw new BadRequestException("El código de reseña no coincide");
        }

        String comment = request.comment() == null ? null : request.comment().trim();
        if (comment != null && comment.length() > MAX_COMMENT) {
            throw new BadRequestException("El comentario no puede superar los " + MAX_COMMENT + " caracteres");
        }

        Review review = reviewRepository.save(Review.builder()
                .connection(connection)
                .student(connection.getStudent())
                .tutor(connection.getTutor())
                .rating(rating)
                .comment(comment == null || comment.isEmpty() ? null : comment)
                .build());

        notificationService.notify(connection.getTutor(), connection.getStudent(),
                NotificationType.REVIEW, review.getId(),
                "@" + connection.getStudent().getUsername() + " te dejó una reseña de " + rating + "★");
        return mapper.toReviewDto(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> byTutor(String username) {
        User tutor = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Preparador no encontrado: " + username));
        return reviewRepository.findByTutor_IdOrderByCreatedAtDesc(tutor.getId()).stream()
                .map(mapper::toReviewDto)
                .toList();
    }
}
