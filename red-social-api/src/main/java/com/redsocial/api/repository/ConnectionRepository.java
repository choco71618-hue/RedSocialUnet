package com.redsocial.api.repository;

import com.redsocial.api.domain.Connection;
import com.redsocial.common.dto.ConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    Optional<Connection> findByStudent_IdAndTutor_Id(Long studentId, Long tutorId);

    Optional<Connection> findByReviewCode(String reviewCode);

    boolean existsByStudent_IdAndTutor_Id(Long studentId, Long tutorId);

    List<Connection> findByStudent_IdOrderByCreatedAtDesc(Long studentId);

    List<Connection> findByTutor_IdOrderByCreatedAtDesc(Long tutorId);

    long countByStudent_IdAndStatus(Long studentId, ConnectionStatus status);

    long countByTutor_IdAndStatus(Long tutorId, ConnectionStatus status);
}
