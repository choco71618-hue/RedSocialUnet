package com.redsocial.api.service;

import com.redsocial.api.domain.Donation;
import com.redsocial.api.domain.Project;
import com.redsocial.api.domain.User;
import com.redsocial.api.exception.BadRequestException;
import com.redsocial.api.exception.ForbiddenException;
import com.redsocial.api.exception.ResourceNotFoundException;
import com.redsocial.api.mapper.DtoMapper;
import com.redsocial.api.repository.DonationRepository;
import com.redsocial.api.repository.ProjectRepository;
import com.redsocial.api.repository.UserRepository;
import com.redsocial.common.dto.CreateProjectRequest;
import com.redsocial.common.dto.DonateRequest;
import com.redsocial.common.dto.ProjectDto;
import com.redsocial.common.dto.ProjectKind;
import com.redsocial.common.dto.ProjectStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class ProjectService {

    private static final int MAX_TITLE = 160;
    private static final int MAX_DESCRIPTION = 4000;

    private final ProjectRepository projectRepository;
    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final DtoMapper mapper;
    private final NotificationService notificationService;

    public ProjectService(ProjectRepository projectRepository,
                          DonationRepository donationRepository,
                          UserRepository userRepository,
                          UserService userService,
                          DtoMapper mapper,
                          NotificationService notificationService) {
        this.projectRepository = projectRepository;
        this.donationRepository = donationRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.mapper = mapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public ProjectDto create(Long currentUserId, CreateProjectRequest request) {
        ProjectKind kind = request.kind();
        if (kind == null) {
            throw new BadRequestException("El tipo de proyecto es obligatorio (EVENT, DONATION o PROJECT)");
        }
        String title = request.title() == null ? null : request.title().trim();
        String description = request.description() == null ? null : request.description().trim();
        if (title == null || title.isEmpty()) {
            throw new BadRequestException("El título es obligatorio");
        }
        if (title.length() > MAX_TITLE) {
            throw new BadRequestException("El título no puede superar los " + MAX_TITLE + " caracteres");
        }
        if (description == null || description.isEmpty()) {
            throw new BadRequestException("La descripción es obligatoria");
        }
        if (description.length() > MAX_DESCRIPTION) {
            throw new BadRequestException("La descripción no puede superar los " + MAX_DESCRIPTION + " caracteres");
        }

        User creator = userService.getEntity(currentUserId);

        BigDecimal targetAmount = null;
        if (kind == ProjectKind.DONATION) {
            targetAmount = request.targetAmount();
            validateDonationTarget(targetAmount);
        }

        ProjectStatus status = null;
        if (kind == ProjectKind.PROJECT) {
            status = request.status() == null ? ProjectStatus.PROPOSED : request.status();
        }

        Project project = projectRepository.save(Project.builder()
                .creator(creator)
                .kind(kind)
                .title(title)
                .description(description)
                .eventDate(request.eventDate())
                .location(request.location() == null ? null : request.location().trim())
                .targetAmount(targetAmount)
                .raisedAmount(BigDecimal.ZERO)
                .status(status)
                .build());
        return toDto(project);
    }

    @Transactional(readOnly = true)
    public ProjectDto get(Long projectId) {
        return toDto(getEntity(projectId));
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> list(ProjectKind kind, ProjectStatus status, String creatorUsername) {
        List<Project> projects = resolve(kind, status, creatorUsername);
        return projects.stream().map(this::toDto).toList();
    }

    @Transactional
    public ProjectDto donate(Long currentUserId, Long projectId, DonateRequest request) {
        Project project = getEntity(projectId);
        if (project.getKind() != ProjectKind.DONATION) {
            throw new BadRequestException("Solo las campañas de donación aceptan aportes");
        }
        BigDecimal amount = request.amount();
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("El monto debe ser mayor a cero");
        }
        if (amount.scale() > 2) {
            throw new BadRequestException("El monto admite como máximo dos decimales");
        }

        User donor = userService.getEntity(currentUserId);
        donationRepository.save(Donation.builder()
                .project(project)
                .donor(donor)
                .amount(amount)
                .build());
        notificationService.notify(project.getCreator(), donor,
                com.redsocial.common.dto.NotificationType.DONATION, projectId,
                "@" + donor.getUsername() + " aportó $" + amount.longValue() + " a «" + project.getTitle() + "»");

        BigDecimal raised = project.getRaisedAmount() == null
                ? BigDecimal.ZERO
                : project.getRaisedAmount();
        project.setRaisedAmount(raised.add(amount));
        projectRepository.save(project);
        return toDto(project);
    }

    @Transactional
    public void delete(Long currentUserId, Long projectId) {
        Project project = getEntity(projectId);
        if (!project.getCreator().getId().equals(currentUserId)) {
            throw new ForbiddenException("Solo el creador puede eliminar el proyecto");
        }
        donationRepository.deleteByProject_Id(projectId);
        projectRepository.delete(project);
    }

    private List<Project> resolve(ProjectKind kind, ProjectStatus status, String creatorUsername) {
        var pageable = PageRequest.of(0, 200);
        if (kind == ProjectKind.EVENT) {
            List<Project> all = projectRepository.findByKindOrderByCreatedAtDesc(ProjectKind.EVENT, pageable);
            return all.stream()
                    .sorted(Comparator.comparing(Project::getEventDate,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
        }
        if (kind != null) {
            return projectRepository.findByKindOrderByCreatedAtDesc(kind, pageable);
        }
        if (status != null) {
            return projectRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        if (creatorUsername != null) {
            return userRepository.findByUsernameIgnoreCase(creatorUsername)
                    .map(user -> projectRepository.findByCreator_IdOrderByCreatedAtDesc(user.getId(), pageable))
                    .orElseGet(List::of);
        }
        return projectRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    private void validateDonationTarget(BigDecimal targetAmount) {
        if (targetAmount == null || targetAmount.signum() <= 0) {
            throw new BadRequestException("La campaña de donación requiere una meta mayor a cero");
        }
        if (targetAmount.scale() > 2) {
            throw new BadRequestException("La meta admite como máximo dos decimales");
        }
    }

    private Project getEntity(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> ResourceNotFoundException.of("Proyecto", projectId));
    }

    private ProjectDto toDto(Project project) {
        long donationCount = project.getKind() == ProjectKind.DONATION
                ? donationRepository.countByProject_Id(project.getId())
                : 0;
        return mapper.toProjectDto(project, donationCount);
    }
}