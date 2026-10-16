package com.redsocial.api.service;

import com.redsocial.api.domain.Connection;
import com.redsocial.api.domain.User;
import com.redsocial.api.exception.BadRequestException;
import com.redsocial.api.exception.ConflictException;
import com.redsocial.api.exception.ForbiddenException;
import com.redsocial.api.exception.ResourceNotFoundException;
import com.redsocial.api.mapper.DtoMapper;
import com.redsocial.api.repository.ConnectionRepository;
import com.redsocial.api.repository.UserRepository;
import com.redsocial.common.dto.ConnectionDto;
import com.redsocial.common.dto.ConnectionStatus;
import com.redsocial.common.dto.CreateConnectionRequest;
import com.redsocial.common.dto.NotificationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ConnectionService {

    private static final String ROLE_STUDENT = "STUDENT";
    private static final String ROLE_TUTOR = "TUTOR";
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;
    private final NotificationService notificationService;
    private final DtoMapper mapper;

    public ConnectionService(ConnectionRepository connectionRepository,
                             UserRepository userRepository,
                             ChatService chatService,
                             NotificationService notificationService,
                             DtoMapper mapper) {
        this.connectionRepository = connectionRepository;
        this.userRepository = userRepository;
        this.chatService = chatService;
        this.notificationService = notificationService;
        this.mapper = mapper;
    }

    @Transactional
    public ConnectionDto request(Long studentId, CreateConnectionRequest request) {
        if (request.tutorId() == null) {
            throw new BadRequestException("El preparador es obligatorio");
        }
        if (request.tutorId().equals(studentId)) {
            throw new BadRequestException("No podés solicitar una conexión a vos mismo");
        }
        User student = getEntity(studentId);
        User tutor = getEntity(request.tutorId());
        if (!ROLE_STUDENT.equals(student.getRole())) {
            throw new ForbiddenException("Solo los estudiantes pueden solicitar conexiones");
        }
        if (!ROLE_TUTOR.equals(tutor.getRole())) {
            throw new BadRequestException("El usuario seleccionado no es un preparador");
        }
        if (connectionRepository.existsByStudent_IdAndTutor_Id(studentId, tutor.getId())) {
            throw new ConflictException("Ya enviaste una solicitud a este preparador");
        }

        Connection connection = connectionRepository.save(Connection.builder()
                .student(student)
                .tutor(tutor)
                .status(ConnectionStatus.PENDING)
                .reviewCode(generateReviewCode())
                .build());

        notificationService.notify(tutor, student, NotificationType.CONNECTION, connection.getId(),
                "@" + student.getUsername() + " quiere conectarse con vos");
        return mapper.toConnectionDto(connection);
    }

    @Transactional
    public ConnectionDto accept(Long tutorId, Long connectionId) {
        Connection connection = requireTutorConnection(tutorId, connectionId);
        if (connection.getStatus() != ConnectionStatus.PENDING) {
            throw new BadRequestException("La solicitud ya fue resuelta");
        }
        connection.setStatus(ConnectionStatus.ACCEPTED);
        connection.setAcceptedAt(Instant.now());
        connectionRepository.save(connection);

        chatService.startConversation(tutorId, connection.getStudent().getId());
        notificationService.notify(connection.getStudent(), connection.getTutor(),
                NotificationType.CONNECTION, connection.getId(),
                "@" + connection.getTutor().getUsername() + " aceptó tu solicitud de conexión");
        return mapper.toConnectionDto(connection);
    }

    @Transactional
    public ConnectionDto reject(Long tutorId, Long connectionId) {
        Connection connection = requireTutorConnection(tutorId, connectionId);
        if (connection.getStatus() != ConnectionStatus.PENDING) {
            throw new BadRequestException("La solicitud ya fue resuelta");
        }
        connection.setStatus(ConnectionStatus.REJECTED);
        connectionRepository.save(connection);
        notificationService.notify(connection.getStudent(), connection.getTutor(),
                NotificationType.CONNECTION, connection.getId(),
                "@" + connection.getTutor().getUsername() + " rechazó tu solicitud de conexión");
        return mapper.toConnectionDto(connection);
    }

    @Transactional(readOnly = true)
    public List<ConnectionDto> mine(Long userId) {
        List<Connection> all = new ArrayList<>();
        all.addAll(connectionRepository.findByStudent_IdOrderByCreatedAtDesc(userId));
        all.addAll(connectionRepository.findByTutor_IdOrderByCreatedAtDesc(userId));
        all.sort(Comparator.comparing(Connection::getCreatedAt).reversed());
        return all.stream().map(mapper::toConnectionDto).toList();
    }

    private Connection requireTutorConnection(Long tutorId, Long connectionId) {
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Conexión", connectionId));
        if (!connection.getTutor().getId().equals(tutorId)) {
            throw new ForbiddenException("Solo el preparador de la solicitud puede responderla");
        }
        return connection;
    }

    private User getEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", id));
    }

    private String generateReviewCode() {
        String code;
        do {
            code = randomCode();
        } while (connectionRepository.findByReviewCode(code).isPresent());
        return code;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
