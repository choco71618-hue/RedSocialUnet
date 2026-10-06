package com.redsocial.api.repository;

import com.redsocial.api.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPost_IdOrderByCreatedAtAsc(Long postId, Pageable pageable);

    long countByPost_Id(Long postId);

    void deleteByPost_Id(Long postId);
}
