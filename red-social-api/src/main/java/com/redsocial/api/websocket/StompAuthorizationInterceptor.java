package com.redsocial.api.websocket;

import com.redsocial.api.repository.ConversationParticipantRepository;
import com.redsocial.api.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ACL de suscripción STOMP: nadie puede recibir mensajes de una conversación
 * de la que no es participante. Aplica a {@code /topic/conversations.{id}} y
 * {@code /topic/conversations.{id}.typing}.
 */
@Component
public class StompAuthorizationInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthorizationInterceptor.class);
    private static final Pattern CONVERSATION_TOPIC =
            Pattern.compile("^/topic/conversations\\.(\\d+)(?:\\.typing)?$");

    private final ConversationParticipantRepository participantRepository;

    public StompAuthorizationInterceptor(ConversationParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }
        String destination = accessor.getDestination();
        if (destination == null) {
            return message;
        }
        Matcher matcher = CONVERSATION_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            return message;
        }
        Long conversationId = Long.valueOf(matcher.group(1));
        Long userId = currentUserId(accessor);
        if (userId == null
                || participantRepository.findByConversation_IdAndUser_Id(conversationId, userId).isEmpty()) {
            log.warn("Suscripción STOMP denegada: usuario={} destino={}", userId, destination);
            throw new AccessDeniedException("No participás en esta conversación");
        }
        return message;
    }

    private Long currentUserId(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof UserPrincipal user) {
            return user.id();
        }
        return null;
    }
}
