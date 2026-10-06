package com.redsocial.api.repository;

import com.redsocial.api.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("""
            select c from Conversation c
            join ConversationParticipant p on p.conversation = c
            where p.user.id = :userId
            order by coalesce(c.lastMessageAt, c.createdAt) desc
            """)
    List<Conversation> findAllForUser(@Param("userId") Long userId);

    @Query("""
            select c from Conversation c
            join ConversationParticipant pa on pa.conversation = c and pa.user.id = :userA
            join ConversationParticipant pb on pb.conversation = c and pb.user.id = :userB
            """)
    List<Conversation> findBetweenUsers(@Param("userA") Long userA, @Param("userB") Long userB);
}
