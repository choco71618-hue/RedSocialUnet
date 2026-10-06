package com.redsocial.api.repository;

import com.redsocial.api.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPost_IdAndUser_Id(Long postId, Long userId);

    Optional<PostLike> findByPost_IdAndUser_Id(Long postId, Long userId);

    long countByPost_Id(Long postId);

    void deleteByPost_IdAndUser_Id(Long postId, Long userId);

    void deleteByPost_Id(Long postId);
}
