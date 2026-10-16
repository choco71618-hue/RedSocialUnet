package com.redsocial.api.controller;

import com.redsocial.api.security.UserPrincipal;
import com.redsocial.api.service.ChatService;
import com.redsocial.common.dto.ConversationDto;
import com.redsocial.common.dto.MessageDto;
import com.redsocial.common.dto.PageResponse;
import com.redsocial.common.dto.SendMessageRequest;
import com.redsocial.common.dto.StartConversationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public List<ConversationDto> list(@AuthenticationPrincipal UserPrincipal me) {
        return chatService.listConversations(me.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationDto start(@AuthenticationPrincipal UserPrincipal me,
                                  @Valid @RequestBody StartConversationRequest request) {
        return chatService.startConversation(me.id(), request.recipientId());
    }

    @GetMapping("/{id}/messages")
    public PageResponse<MessageDto> messages(@AuthenticationPrincipal UserPrincipal me,
                                             @PathVariable Long id,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size) {
        return chatService.getMessages(me.id(), id, page, size);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDto send(@AuthenticationPrincipal UserPrincipal me,
                           @PathVariable Long id,
                            @Valid @RequestBody SendMessageRequest request) {
        return chatService.send(me.id(), id, request.content());
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        chatService.markRead(me.id(), id);
    }
}
