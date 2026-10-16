package com.redsocial.api.controller;

import com.redsocial.api.security.UserPrincipal;
import com.redsocial.api.service.ProjectService;
import com.redsocial.common.dto.CreateProjectRequest;
import com.redsocial.common.dto.DonateRequest;
import com.redsocial.common.dto.ProjectDto;
import com.redsocial.common.dto.ProjectKind;
import com.redsocial.common.dto.ProjectStatus;
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

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectDto> list(@RequestParam(required = false) ProjectKind kind,
                                 @RequestParam(required = false) ProjectStatus status,
                                 @RequestParam(required = false) String creator) {
        return projectService.list(kind, status, creator);
    }

    @GetMapping("/{id}")
    public ProjectDto get(@PathVariable Long id) {
        return projectService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDto create(@AuthenticationPrincipal UserPrincipal me,
                              @Valid @RequestBody CreateProjectRequest request) {
        return projectService.create(me.id(), request);
    }

    @PostMapping("/{id}/donate")
    public ProjectDto donate(@AuthenticationPrincipal UserPrincipal me,
                             @PathVariable Long id,
                              @Valid @RequestBody DonateRequest request) {
        return projectService.donate(me.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        projectService.delete(me.id(), id);
    }
}