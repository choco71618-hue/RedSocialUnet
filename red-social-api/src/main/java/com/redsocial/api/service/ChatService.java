package com.redsocial.api.service;

import com.redsocial.api.domain.Conversation;
import com.redsocial.api.domain.ConversationParticipant;
import com.redsocial.api.domain.Message;
import com.redsocial.api.domain.User;
import com.redsocial.api.exception.BadRequestException;
import com.redsocial.api.exception.ForbiddenException;
import com.redsocial.api.exception.ResourceNotFoundException;
import com.redsocial.api.mapper.DtoMapper;
import com.redsocial.api.repository.ConversationParticipantRepository;
import com.redsocial.api.repository.ConversationRepository;
import com.redsocial.api.repository.MessageRepository;
import com.redsocial.api.repository.UserRepository;
import com.redsocial.common.dto.ConversationDto;
import com.redsocial.common.dto.MessageDto;
import com.redsocial.common.dto.PageResponse;
import com.redsocial.common.dto.SendMessageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ChatService {

    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final DtoMapper mapper;
    private final NotificationService notificationService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public ChatService(ConversationRepository conversationRepository,
                       ConversationParticipantRepository participantRepository,
                       MessageRepository messageRepository,
                       UserRepository userRepository,
                       UserService userService,
                       DtoMapper mapper,
                       NotificationService notificationService,
                       org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.mapper = mapper;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public ConversationDto startConversation(Long currentUserId, Long recipientId) {
        if (recipientId == null) {
            throw new BadRequestException("El destinatario es obligatorio");
        }
        if (recipientId.equals(currentUserId)) {
            throw new BadRequestException("No podés iniciar una conversación con vos mismo");
        }
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", recipientId));
        Conversation conversation = getOrCreate(currentUserId, recipientId);
        return toDto(conversation, currentUserId, recipient);
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> listConversations(Long currentUserId) {
        return conversationRepository.findAllForUser(currentUserId).stream()
                .map(conversation -> toDtoForList(conversation, currentUserId))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageDto> getMessages(Long currentUserId, Long conversationId, int page, int size) {
        requireParticipant(conversationId, currentUserId);
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId, pageable);
        return PageResponse.of(
                messages.getContent().stream().map(mapper::toMessageDto).toList(),
                messages.getNumber(),
                messages.getSize(),
                messages.getTotalElements());
    }

    @Transactional
    public MessageDto send(Long senderId, SendMessageRequest request) {
        Conversation conversation;
        if (request.conversationId() != null) {
            conversation = conversationRepository.findById(request.conversationId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Conversación", request.conversationId()));
            requireParticipant(conversation.getId(), senderId);
        } else if (request.recipientId() != null) {
            if (request.recipientId().equals(senderId)) {
                throw new BadRequestException("No podés enviarte un mensaje a vos mismo");
            }
            conversation = getOrCreate(senderId, request.recipientId());
        } else {
            throw new BadRequestException("Se requiere conversationId o recipientId");
        }
        return doSend(senderId, conversation, request.content());
    }

    @Transactional
    public MessageDto send(Long senderId, Long conversationId, String content) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Conversación", conversationId));
        requireParticipant(conversationId, senderId);
        return doSend(senderId, conversation, content);
    }

    @Transactional
    public void markRead(Long currentUserId, Long conversationId) {
        requireParticipant(conversationId, currentUserId);
        List<Message> unread = messageRepository
                .findByConversation_IdAndSender_IdNotAndReadAtIsNull(conversationId, currentUserId);
        Instant now = Instant.now();
        unread.forEach(message -> message.setReadAt(now));
        messageRepository.saveAll(unread);
    }

    private MessageDto doSend(Long senderId, Conversation conversation, String content) {
        String text = content == null ? null : content.trim();
        if (text == null || text.isEmpty()) {
            throw new BadRequestException("El mensaje no puede estar vacío");
        }
        if (text.length() > MAX_MESSAGE_LENGTH) {
            throw new BadRequestException("El mensaje no puede superar los " + MAX_MESSAGE_LENGTH + " caracteres");
        }
        Message message = messageRepository.save(Message.builder()
                .conversation(conversation)
                .sender(userService.getEntity(senderId))
                .content(text)
                .build());
        conversation.setLastMessageAt(message.getCreatedAt());
        conversationRepository.save(conversation);
        User sender = userService.getEntity(senderId);
        otherParticipant(conversation, senderId).ifPresent(other ->
                notificationService.notify(other, sender,
                        com.redsocial.common.dto.NotificationType.MESSAGE, conversation.getId(),
                        "@" + sender.getUsername() + " te envió un mensaje"));
        MessageDto dto = mapper.toMessageDto(message);
        messagingTemplate.convertAndSend("/topic/conversations." + conversation.getId(), dto);
        return dto;
    }

    private Conversation getOrCreate(Long userA, Long userB) {
        List<Conversation> existing = conversationRepository.findBetweenUsers(userA, userB);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        Conversation conversation = conversationRepository.save(Conversation.builder().build());
        participantRepository.save(ConversationParticipant.builder()
                .conversation(conversation)
                .user(userRepository.getReferenceById(userA))
                .build());
        participantRepository.save(ConversationParticipant.builder()
                .conversation(conversation)
                .user(userRepository.getReferenceById(userB))
                .build());
        return conversation;
    }

    private void requireParticipant(Long conversationId, Long userId) {
        if (participantRepository.findByConversation_IdAndUser_Id(conversationId, userId).isEmpty()) {
            throw new ForbiddenException("No participás en esta conversación");
        }
    }

    private java.util.Optional<User> otherParticipant(Conversation conversation, Long currentUserId) {
        return participantRepository.findByConversation_Id(conversation.getId()).stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> !user.getId().equals(currentUserId))
                .findFirst();
    }

    private ConversationDto toDtoForList(Conversation conversation, Long currentUserId) {
        User other = participantRepository.findByConversation_Id(conversation.getId()).stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> !user.getId().equals(currentUserId))
                .findFirst()
                .orElse(null);
        if (other == null) {
            return null;
        }
        return toDto(conversation, currentUserId, other);
    }

    private ConversationDto toDto(Conversation conversation, Long currentUserId, User other) {
        Message last = messageRepository.findTopByConversation_IdOrderByCreatedAtDesc(conversation.getId())
                .orElse(null);
        long unread = messageRepository
                .countByConversation_IdAndSender_IdNotAndReadAtIsNull(conversation.getId(), currentUserId);
        return mapper.toConversationDto(
                conversation,
                other,
                last == null ? null : last.getContent(),
                last == null ? null : last.getCreatedAt(),
                unread);
    }
}
