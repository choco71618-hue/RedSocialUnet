package com.redsocial.api.controller;

import com.redsocial.api.security.UserPrincipal;
import com.redsocial.api.service.ReviewService;
import com.redsocial.api.service.TutorService;
import com.redsocial.common.dto.ReviewDto;
import com.redsocial.common.dto.TutorDto;
import com.redsocial.common.dto.TutorType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tutors")
public class TutorController {

    private final TutorService tutorService;
    private final ReviewService reviewService;

    public TutorController(TutorService tutorService, ReviewService reviewService) {
        this.tutorService = tutorService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public List<TutorDto> search(@AuthenticationPrincipal UserPrincipal me,
                                 @RequestParam(required = false) String q,
                                 @RequestParam(required = false) String subject,
                                 @RequestParam(required = false) TutorType tutorType) {
        return tutorService.search(q, subject, tutorType, me.id());
    }

    @GetMapping("/{username}")
    public TutorDto get(@AuthenticationPrincipal UserPrincipal me, @PathVariable String username) {
        return tutorService.get(username, me.id());
    }

    @GetMapping("/{username}/reviews")
    public List<ReviewDto> reviews(@PathVariable String username) {
        return reviewService.byTutor(username);
    }
}
