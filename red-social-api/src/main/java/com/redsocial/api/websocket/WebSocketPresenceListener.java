package com.redsocial.api.websocket;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Optional;

@Component
public class WebSocketPresenceListener {

    private final PresenceRegistry presenceRegistry;

    public WebSocketPresenceListener(PresenceRegistry presenceRegistry) {
        this.presenceRegistry = presenceRegistry;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        username(event).ifPresent(presenceRegistry::markConnected);
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        username(event).ifPresent(presenceRegistry::markDisconnected);
    }

    private Optional<String> username(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        return username(from(accessor.getUser()));
    }

    private Optional<String> username(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        return username(from(accessor.getUser()));
    }

    private Authentication from(java.security.Principal principal) {
        return principal instanceof Authentication authentication ? authentication : null;
    }

    private Optional<String> username(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.redsocial.api.security.UserPrincipal userPrincipal) {
            return Optional.of(userPrincipal.getUsername());
        }
        return Optional.empty();
    }
}