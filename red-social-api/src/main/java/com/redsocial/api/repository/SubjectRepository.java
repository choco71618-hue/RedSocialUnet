package com.redsocial.api.repository;

import com.redsocial.api.domain.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByNameIgnoreCase(String name);

    List<Subject> findAllByOrderByNameAsc();
}
