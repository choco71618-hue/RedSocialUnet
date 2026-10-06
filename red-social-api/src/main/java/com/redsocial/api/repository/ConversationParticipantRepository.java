package com.redsocial.api.repository;

import com.redsocial.api.domain.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    List<ConversationParticipant> findByConversation_Id(Long conversationId);

    Optional<ConversationParticipant> findByConversation_IdAndUser_Id(Long conversationId, Long userId);
}
