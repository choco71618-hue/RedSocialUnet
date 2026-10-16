package com.redsocial.api.service;

import com.redsocial.api.domain.Connection;
import com.redsocial.api.domain.Subject;
import com.redsocial.api.domain.TutorSubject;
import com.redsocial.api.domain.User;
import com.redsocial.api.exception.BadRequestException;
import com.redsocial.api.exception.ConflictException;
import com.redsocial.api.exception.ForbiddenException;
import com.redsocial.api.exception.ResourceNotFoundException;
import com.redsocial.api.mapper.DtoMapper;
import com.redsocial.api.repository.ConnectionRepository;
import com.redsocial.api.repository.ReviewRepository;
import com.redsocial.api.repository.SubjectRepository;
import com.redsocial.api.repository.TutorSubjectRepository;
import com.redsocial.api.repository.UserRepository;
import com.redsocial.common.dto.ConnectionStatus;
import com.redsocial.common.dto.SaveTutorSubjectRequest;
import com.redsocial.common.dto.SubjectDto;
import com.redsocial.common.dto.TutorDto;
import com.redsocial.common.dto.TutorSubjectDto;
import com.redsocial.common.dto.TutorType;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TutorService {

    private static final String ROLE_TUTOR = "TUTOR";
    private static final int MAX_LIST = 200;
    private static final int MAX_NAME = 120;
    private static final int MAX_PRICE_SCALE = 2;

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final TutorSubjectRepository tutorSubjectRepository;
    private final ReviewRepository reviewRepository;
    private final ConnectionRepository connectionRepository;
    private final DtoMapper mapper;

    public TutorService(UserRepository userRepository,
                        SubjectRepository subjectRepository,
                        TutorSubjectRepository tutorSubjectRepository,
                        ReviewRepository reviewRepository,
                        ConnectionRepository connectionRepository,
                        DtoMapper mapper) {
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.tutorSubjectRepository = tutorSubjectRepository;
        this.reviewRepository = reviewRepository;
        this.connectionRepository = connectionRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<TutorDto> search(String q, String subject, TutorType tutorType, Long currentUserId) {
        String name = normalize(q);
        String subjectQuery = normalize(subject);
        return userRepository.findByRoleOrderByCreatedAtAsc(ROLE_TUTOR, PageRequest.of(0, MAX_LIST)).stream()
                .filter(tutor -> tutorType == null || tutorType.equals(tutor.getTutorType()))
                .filter(tutor -> matchesName(tutor, name))
                .filter(tutor -> matchesSubject(tutor, subjectQuery))
                .map(tutor -> toTutorDto(tutor, currentUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public TutorDto get(String username, Long currentUserId) {
        User tutor = findByUsername(username);
        if (!ROLE_TUTOR.equals(tutor.getRole())) {
            throw new ResourceNotFoundException("Preparador no encontrado: " + username);
        }
        return toTutorDto(tutor, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<SubjectDto> catalog() {
        return subjectRepository.findAllByOrderByNameAsc().stream().map(mapper::toSubjectDto).toList();
    }

    @Transactional(readOnly = true)
    public List<TutorSubjectDto> mySubjects(Long tutorId) {
        return loadSubjects(tutorId);
    }

    @Transactional
    public TutorSubjectDto addSubject(Long tutorId, SaveTutorSubjectRequest request) {
        String name = request.subjectName() == null ? null : request.subjectName().trim();
        if (name == null || name.isEmpty()) {
            throw new BadRequestException("El nombre de la materia es obligatorio");
        }
        if (name.length() > MAX_NAME) {
            throw new BadRequestException("El nombre de la materia no puede superar los " + MAX_NAME + " caracteres");
        }
        BigDecimal price = request.pricePerHour();
        if (price == null || price.signum() <= 0) {
            throw new BadRequestException("El precio por hora debe ser mayor a cero");
        }
        if (price.scale() > MAX_PRICE_SCALE) {
            throw new BadRequestException("El precio admite como máximo dos decimales");
        }

        User tutor = getEntity(tutorId);
        Subject subject = subjectRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> subjectRepository.save(Subject.builder().name(name).build()));

        if (tutorSubjectRepository.findByTutor_IdAndSubject_Id(tutorId, subject.getId()).isPresent()) {
            throw new ConflictException("Ya ofrecés esa materia");
        }

        TutorSubject saved = tutorSubjectRepository.save(TutorSubject.builder()
                .tutor(tutor)
                .subject(subject)
                .pricePerHour(price)
                .build());
        return mapper.toTutorSubjectDto(saved);
    }

    @Transactional
    public void removeSubject(Long tutorId, Long tutorSubjectId) {
        TutorSubject ts = tutorSubjectRepository.findById(tutorSubjectId)
                .orElseThrow(() -> ResourceNotFoundException.of("Materia del preparador", tutorSubjectId));
        if (!ts.getTutor().getId().equals(tutorId)) {
            throw new ForbiddenException("Solo podés quitar tus propias materias");
        }
        tutorSubjectRepository.delete(ts);
    }

    private User findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Preparador no encontrado: " + username));
    }

    private User getEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", id));
    }

    private boolean matchesName(User tutor, String name) {
        if (name == null) {
            return true;
        }
        String haystack = ((tutor.getDisplayName() == null ? "" : tutor.getDisplayName())
                + " " + tutor.getUsername()).toLowerCase();
        return haystack.contains(name);
    }

    private boolean matchesSubject(User tutor, String subjectQuery) {
        if (subjectQuery == null) {
            return true;
        }
        return loadSubjects(tutor.getId()).stream()
                .anyMatch(s -> s.subjectName().toLowerCase().contains(subjectQuery));
    }

    private List<TutorSubjectDto> loadSubjects(Long tutorId) {
        return tutorSubjectRepository.findByTutor_IdOrderBySubject_NameAsc(tutorId).stream()
                .map(mapper::toTutorSubjectDto)
                .toList();
    }

    private TutorDto toTutorDto(User tutor, Long currentUserId) {
        List<TutorSubjectDto> subjects = loadSubjects(tutor.getId());
        Double average = reviewRepository.averageByTutor(tutor.getId());
        long reviewCount = reviewRepository.countByTutor_Id(tutor.getId());
        ConnectionStatus status = null;
        if (currentUserId != null) {
            status = connectionRepository.findByStudent_IdAndTutor_Id(currentUserId, tutor.getId())
                    .map(Connection::getStatus)
                    .orElse(null);
        }
        return mapper.toTutorDto(tutor, subjects, average == null ? 0.0 : average, reviewCount, status);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toLowerCase();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
