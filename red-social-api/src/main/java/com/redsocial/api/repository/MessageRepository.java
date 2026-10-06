package com.redsocial.api.repository;

import com.redsocial.api.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversation_IdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);

    Optional<Message> findTopByConversation_IdOrderByCreatedAtDesc(Long conversationId);

    long countByConversation_IdAndSender_IdNotAndReadAtIsNull(Long conversationId, Long userId);

    List<Message> findByConversation_IdAndSender_IdNotAndReadAtIsNull(Long conversationId, Long userId);
}
