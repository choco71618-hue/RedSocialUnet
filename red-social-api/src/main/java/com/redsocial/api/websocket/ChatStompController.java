package com.redsocial.api.websocket;

import com.redsocial.api.security.UserPrincipal;
import com.redsocial.api.service.ChatService;
import com.redsocial.common.dto.MessageDto;
import com.redsocial.common.dto.SendMessageRequest;
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
        MessageDto message = chatService.send(user.id(), request);
        messagingTemplate.convertAndSend("/topic/conversations." + message.conversationId(), message);
    }

    private UserPrincipal resolve(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken authentication
                && authentication.getPrincipal() instanceof UserPrincipal user) {
            return user;
        }
        return null;
    }
}
