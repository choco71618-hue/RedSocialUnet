package com.redsocial.api.repository;

import com.redsocial.api.domain.Project;
import com.redsocial.common.dto.ProjectKind;
import com.redsocial.common.dto.ProjectStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByKindOrderByCreatedAtDesc(ProjectKind kind, Pageable pageable);

    List<Project> findByStatusOrderByCreatedAtDesc(ProjectStatus status, Pageable pageable);

    List<Project> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Project> findByCreator_IdOrderByCreatedAtDesc(Long creatorId, Pageable pageable);

    long countByKind(ProjectKind kind);
}