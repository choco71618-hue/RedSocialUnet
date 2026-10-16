package com.redsocial.api.repository;

import com.redsocial.api.domain.Friendship;
import com.redsocial.common.dto.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByRequester_IdAndAddressee_Id(Long requesterId, Long addresseeId);

    Optional<Friendship> findByIdAndAddressee_Id(Long id, Long addresseeId);

    Optional<Friendship> findByIdAndRequester_Id(Long id, Long requesterId);

    @Query("""
            select f from Friendship f
            where (f.requester.id = :a and f.addressee.id = :b)
               or (f.requester.id = :b and f.addressee.id = :a)
            """)
    Optional<Friendship> findBetween(@Param("a") Long a, @Param("b") Long b);

    @Query("""
            select f from Friendship f
            where f.status = :status
              and (f.requester.id = :userId or f.addressee.id = :userId)
            """)
    List<Friendship> findAllFor(@Param("userId") Long userId, @Param("status") FriendshipStatus status);

    List<Friendship> findByAddressee_IdAndStatusOrderByCreatedAtDesc(Long addresseeId, FriendshipStatus status);

    List<Friendship> findByRequester_IdAndStatusOrderByCreatedAtDesc(Long requesterId, FriendshipStatus status);

    long countByAddressee_IdAndStatus(Long addresseeId, FriendshipStatus status);
}
