package com.redsocial.api.controller;

import com.redsocial.api.service.TutorService;
import com.redsocial.common.dto.SubjectDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final TutorService tutorService;

    public SubjectController(TutorService tutorService) {
        this.tutorService = tutorService;
    }

    @GetMapping
    public List<SubjectDto> catalog() {
        return tutorService.catalog();
    }
}
