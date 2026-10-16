package com.redsocial.ui.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redsocial.common.dto.MessageDto;
import com.redsocial.common.dto.NotificationDto;
import com.redsocial.common.dto.TypingRequest;
import com.redsocial.ui.config.ApiProperties;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@VaadinSessionScope
public class RealtimeBridge {

    private static final Logger log = LoggerFactory.getLogger(RealtimeBridge.class);

    private final ApiProperties apiProperties;
    private final ObjectMapper objectMapper;
    private WebSocketStompClient client;
    private StompSession session;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private UI ui;

    private final List<Consumer<NotificationDto>> notificationHandlers = new CopyOnWriteArrayList<>();
    private final Map<Long, List<Consumer<MessageDto>>> messageHandlers = new ConcurrentHashMap<>();
    private final Map<String, List<Consumer<TypingInfo>>> typingHandlers = new ConcurrentHashMap<>();
    private final Map<String, StompSession.Subscription> subscriptions = new ConcurrentHashMap<>();

    public RealtimeBridge(ApiProperties apiProperties, ObjectMapper objectMapper) {
        this.apiProperties = apiProperties;
        this.objectMapper = objectMapper;
    }

    public void connect(String token, UI ui) {
        this.ui = ui;
        if (connected.get() || token == null || token.isEmpty()) {
            return;
        }
        String apiBase = apiProperties.baseUrl();                        // http://localhost:8080/api
        String wsUrl = apiBase.replaceFirst("/api$", "/ws").replaceFirst("^http", "ws");
        try {
            client = new WebSocketStompClient(new StandardWebSocketClient());
            MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
            converter.setObjectMapper(objectMapper);
            client.setMessageConverter(converter);
            CountDownLatch latch = new CountDownLatch(1);
            session = client.connectAsync(wsUrl, new org.springframework.web.socket.WebSocketHttpHeaders(),
                    buildStompHeaders(token),
                    new StompSessionHandlerAdapter() {
                        @Override
                        public void handleException(StompSession session, StompCommand command,
                                                    StompHeaders headers, byte[] payload, Throwable exception) {
                            log.warn("STOMP exception: {}", exception.getMessage());
                        }

                        @Override
                        public void handleTransportError(StompSession s, Throwable exception) {
                            connected.set(false);
                            log.warn("STOMP transport error: {}", exception.getMessage());
                        }
                    }).get(10, TimeUnit.SECONDS);
            if (session.isConnected()) {
                connected.set(true);
                subscriptions.clear();
                subscribeTracked("/user/queue/notifications", new NotificationFrameHandler());
                messageHandlers.keySet().forEach(id -> subscribeTracked("/topic/conversations." + id, new MessageFrameHandler(id)));
                typingHandlers.keySet().forEach(channel -> subscribeTracked("/topic/conversations." + channel + ".typing", new TypingFrameHandler(channel)));
                log.info("RealtimeBridge conectado a {}", wsUrl);
            }
        } catch (Exception ex) {
            connected.set(false);
            log.warn("No se pudo conectar STOMP a {}: {}", wsUrl, ex.getMessage());
        }
    }

    public boolean isConnected() {
        return connected.get() && session != null && session.isConnected();
    }

    public void addNotificationHandler(Consumer<NotificationDto> handler) {
        notificationHandlers.add(handler);
    }

    public void removeNotificationHandler(Consumer<NotificationDto> handler) {
        notificationHandlers.remove(handler);
    }

    public void subscribeConversation(Long conversationId, Consumer<MessageDto> handler) {
        if (handler == null) {
            return;
        }
        messageHandlers.computeIfAbsent(conversationId, id -> new CopyOnWriteArrayList<>()).add(handler);
        subscribeTracked("/topic/conversations." + conversationId, new MessageFrameHandler(conversationId));
    }

    public void unsubscribeConversation(Long conversationId) {
        messageHandlers.remove(conversationId);
        unsubscribeTracked("/topic/conversations." + conversationId);
    }

    public void addTypingHandler(String channel, Consumer<TypingInfo> handler) {
        if (handler == null) {
            return;
        }
        typingHandlers.computeIfAbsent(channel, id -> new CopyOnWriteArrayList<>()).add(handler);
        subscribeTracked("/topic/conversations." + channel + ".typing", new TypingFrameHandler(channel));
    }

    public void removeTypingHandler(String channel) {
        typingHandlers.remove(channel);
        unsubscribeTracked("/topic/conversations." + channel + ".typing");
    }

    /**
     * Cierra la conexión STOMP y limpia las suscripciones. Se llama al cerrar
     * sesión para no dejar el socket vivo con el token anterior.
     */
    public void disconnect() {
        subscriptions.values().forEach(subscription -> {
            try {
                subscription.unsubscribe();
            } catch (Exception ignored) {
                // la sesión ya puede estar cerrada
            }
        });
        subscriptions.clear();
        if (session != null && session.isConnected()) {
            try {
                session.disconnect();
            } catch (Exception ignored) {
                // ignorar errores al cerrar
            }
        }
        session = null;
        connected.set(false);
        notificationHandlers.clear();
        messageHandlers.clear();
        typingHandlers.clear();
    }

    private void subscribeTracked(String destination, StompFrameHandler handler) {
        if (!isConnected()) {
            return;
        }
        subscriptions.computeIfAbsent(destination, dest -> session.subscribe(dest, handler));
    }

    private void unsubscribeTracked(String destination) {
        StompSession.Subscription subscription = subscriptions.remove(destination);
        if (subscription == null) {
            return;
        }
        try {
            subscription.unsubscribe();
        } catch (Exception ignored) {
            // la sesión ya puede estar cerrada
        }
    }

    public void sendTyping(Long conversationId, boolean typing) {
        if (!isConnected()) {
            return;
        }
        session.send("/app/chat.typing", new TypingRequest(conversationId, typing));
    }

    private StompHeaders buildStompHeaders(String token) {
        StompHeaders headers = new StompHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return headers;
    }

    private class NotificationFrameHandler implements StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders headers) {
            return NotificationDto.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            NotificationDto dto = (NotificationDto) payload;
            notificationHandlers.forEach(handler -> runInUi(() -> handler.accept(dto)));
        }
    }

    private class MessageFrameHandler implements StompFrameHandler {
        private final Long conversationId;

        MessageFrameHandler(Long conversationId) {
            this.conversationId = conversationId;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return MessageDto.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            MessageDto dto = (MessageDto) payload;
            List<Consumer<MessageDto>> handlers = messageHandlers.get(conversationId);
            if (handlers != null) {
                handlers.forEach(handler -> runInUi(() -> handler.accept(dto)));
            }
        }
    }

    private class TypingFrameHandler implements StompFrameHandler {
        private final String channel;

        TypingFrameHandler(String channel) {
            this.channel = channel;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return com.redsocial.common.dto.TypingEvent.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            com.redsocial.common.dto.TypingEvent dto = (com.redsocial.common.dto.TypingEvent) payload;
            List<Consumer<TypingInfo>> handlers = typingHandlers.get(channel);
            if (handlers != null) {
                handlers.forEach(handler -> runInUi(() -> handler.accept(
                        new TypingInfo(dto.conversationId(), dto.username(), dto.typing()))));
            }
        }
    }

    private void runInUi(Runnable action) {
        UI current = ui != null ? ui : UI.getCurrent();
        if (current != null) {
            current.access((com.vaadin.flow.server.Command) action::run);
        } else {
            action.run();
        }
    }

    public record TypingInfo(Long conversationId, String username, boolean typing) {
    }

    @FunctionalInterface
    public interface Consumer<T> {
        void accept(T value);
    }
}