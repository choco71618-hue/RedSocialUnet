package com.redsocial.api.repository;

import com.redsocial.api.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByTutor_IdOrderByCreatedAtDesc(Long tutorId);

    Optional<Review> findByConnection_Id(Long connectionId);

    boolean existsByConnection_Id(Long connectionId);

    long countByTutor_Id(Long tutorId);

    @Query("select coalesce(avg(r.rating), 0.0) from Review r where r.tutor.id = :tutorId")
    Double averageByTutor(@Param("tutorId") Long tutorId);

    void deleteByTutor_Id(Long tutorId);
}
