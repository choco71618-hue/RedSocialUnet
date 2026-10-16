package com.redsocial.api.repository;

import com.redsocial.api.domain.TutorSubject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TutorSubjectRepository extends JpaRepository<TutorSubject, Long> {

    List<TutorSubject> findByTutor_IdOrderBySubject_NameAsc(Long tutorId);

    Optional<TutorSubject> findByTutor_IdAndSubject_Id(Long tutorId, Long subjectId);

    boolean existsBySubject_Id(Long subjectId);

    void deleteByTutor_Id(Long tutorId);
}
