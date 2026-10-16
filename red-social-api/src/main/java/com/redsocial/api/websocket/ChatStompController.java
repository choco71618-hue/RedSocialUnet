package com.redsocial.api.websocket;

import com.redsocial.api.security.UserPrincipal;
import com.redsocial.api.service.ChatService;
import com.redsocial.common.dto.MessageDto;
import com.redsocial.common.dto.SendMessageRequest;
import com.redsocial.common.dto.TypingEvent;
import com.redsocial.common.dto.TypingRequest;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatStompController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatStompController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void send(@Payload SendMessageRequest request, Principal principal) {
        UserPrincipal user = resolve(principal);
        if (user == null) {
            return;
        }
        chatService.send(user.id(), request);
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingRequest request, Principal principal) {
        UserPrincipal user = resolve(principal);
        if (user == null || request.conversationId() == null) {
            return;
        }
        if (!chatService.isParticipant(request.conversationId(), user.id())) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/conversations." + request.conversationId() + ".typing",
                new TypingEvent(request.conversationId(), user.getUsername(), request.typing()));
    }

    private UserPrincipal resolve(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken authentication
                && authentication.getPrincipal() instanceof UserPrincipal user) {
            return user;
        }
        return null;
    }
}
