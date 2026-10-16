package com.redsocial.api.controller;

import com.redsocial.api.security.UserPrincipal;
import com.redsocial.api.service.TutorService;
import com.redsocial.common.dto.SaveTutorSubjectRequest;
import com.redsocial.common.dto.TutorSubjectDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tutor-subjects")
@PreAuthorize("hasRole('TUTOR')")
public class TutorSubjectController {

    private final TutorService tutorService;

    public TutorSubjectController(TutorService tutorService) {
        this.tutorService = tutorService;
    }

    @GetMapping
    public List<TutorSubjectDto> mine(@AuthenticationPrincipal UserPrincipal me) {
        return tutorService.mySubjects(me.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TutorSubjectDto add(@AuthenticationPrincipal UserPrincipal me,
                               @Valid @RequestBody SaveTutorSubjectRequest request) {
        return tutorService.addSubject(me.id(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        tutorService.removeSubject(me.id(), id);
    }
}
