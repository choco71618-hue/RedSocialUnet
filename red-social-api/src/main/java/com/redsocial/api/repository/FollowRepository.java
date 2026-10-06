package com.redsocial.api.repository;

import com.redsocial.api.domain.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollower_IdAndFollowing_Id(Long followerId, Long followingId);

    Optional<Follow> findByFollower_IdAndFollowing_Id(Long followerId, Long followingId);

    long countByFollowing_Id(Long followingId);

    long countByFollower_Id(Long followerId);

    List<Follow> findByFollowing_Id(Long followingId);

    List<Follow> findByFollower_Id(Long followerId);
}
