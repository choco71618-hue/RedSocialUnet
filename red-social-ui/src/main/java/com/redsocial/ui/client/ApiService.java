package com.redsocial.ui.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redsocial.common.dto.AuthResponse;
import com.redsocial.common.dto.CommentDto;
import com.redsocial.common.dto.ConversationDto;
import com.redsocial.common.dto.CreateCommentRequest;
import com.redsocial.common.dto.CreatePostRequest;
import com.redsocial.common.dto.LoginRequest;
import com.redsocial.common.dto.MessageDto;
import com.redsocial.common.dto.PageResponse;
import com.redsocial.common.dto.PostDto;
import com.redsocial.common.dto.RegisterRequest;
import com.redsocial.common.dto.SendMessageRequest;
import com.redsocial.common.dto.StartConversationRequest;
import com.redsocial.common.dto.UpdateProfileRequest;
import com.redsocial.common.dto.UserDto;
import com.redsocial.ui.config.ApiProperties;
import com.redsocial.ui.session.UserSession;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.function.Consumer;

@Component
public class ApiService {

    private final RestClient client;
    private final UserSession session;
    private final ObjectMapper objectMapper;

    public ApiService(ApiProperties properties, UserSession session, ObjectMapper objectMapper) {
        this.client = RestClient.builder().baseUrl(properties.baseUrl()).build();
        this.session = session;
        this.objectMapper = objectMapper;
    }

    public AuthResponse register(RegisterRequest request) {
        try {
            return client.post().uri("/auth/register").body(request).retrieve().body(AuthResponse.class);
        } catch (Exception ex) {
            throw translate(ex);
        }
    }

    public AuthResponse login(LoginRequest request) {
        try {
            return client.post().uri("/auth/login").body(request).retrieve().body(AuthResponse.class);
        } catch (Exception ex) {
            throw translate(ex);
        }
    }

    public UserDto me() {
        return get("/users/me", new ParameterizedTypeReference<>() {});
    }

    public UserDto updateProfile(UpdateProfileRequest request) {
        try {
            return client.put().uri("/users/me").headers(auth()).body(request).retrieve().body(UserDto.class);
        } catch (Exception ex) {
            throw translate(ex);
        }
    }

    public UserDto profile(String username) {
        return get("/users/" + username, new ParameterizedTypeReference<>() {});
    }

    public List<UserDto> search(String query) {
        return get("/users/search?q=" + query, new ParameterizedTypeReference<>() {});
    }

    public UserDto follow(String username) {
        try {
            return client.post().uri("/users/" + username + "/follow").headers(auth()).retrieve().body(UserDto.class);
        } catch (Exception ex) {
            throw translate(ex);
        }
    }

    public UserDto unfollow(String username) {
        try {
            return client.delete().uri("/users/" + username + "/follow").headers(auth()).retrieve().body(UserDto.class);
        } catch (Exception ex) {
            throw translate(ex);
        }
    }

    public PageResponse<PostDto> feed(int page, int size) {
        return get("/feed?page=" + page + "&size=" + size, new ParameterizedTypeReference<>() {});
    }

    public PageResponse<PostDto> userPosts(String username, int page, int size) {
        return get("/users/" + username + "/posts?page=" + page + "&size=" + size, new ParameterizedTypeReference<>() {});
    }

    public PostDto createPost(String content) {
        try {
            return client.post().uri("/posts").headers(auth())
                    .body(new CreatePostRequest(content)).retrieve().body(PostDto.class);
        } catch (Exception ex) {
            throw translate(ex);
        }
    }

    public PostDto like(Long postId) {
        try {
            return client.post().uri("/posts/" + postId + "/like").headers(auth()).retrieve().body(PostDto.class);
        } catch (Exception ex) {
            throw translate(ex);
        }
    }

    public PostDto unlike(Long postId) {
        try {
            return client.delete().uri("/posts/" + postId + "/like").headers(auth()).retrieve().body(PostDto.class);
        } catch (Exception ex) {
            throw translate(ex);
        }
    }

    public void deletePost(Long postId) {
        try {
            client.delete().uri("/posts/" + postId).headers(auth()).retrieve().toBodilessEntity();
        } catch (Exception ex) {
            throw translate(ex);
        }
    }

    public PageResponse<CommentDto> comments(Long postId, int page, int size) {
        return get("/posts/" + postId + "/comments?page=" + page + "&size=" + size, new ParameterizedTypeReference<>() {});
    }

    public CommentDto addComment(Long postId, String content) {
        try {
            return client.post().uri("/posts/" + postId + "/comments").headers(auth())
                    .body(new CreateCommentRequest(content)).retrieve().body(CommentDto.class);
        } catch (Exception ex) {
            throw translate(ex);
        }
    }

    public List<ConversationDto> conversations() {
        return get("/conversations", new ParameterizedTypeReference<>() {});
    }

    public ConversationDto startConversation(Long recipientId) {
        try {
            return client.post().uri("/conversations").headers(auth())
                    .body(new StartConversationRequest(recipientId)).retrieve().body(ConversationDto.class);
        } catch (Exception ex) {
            throw translate(ex);
        }
    }

    public PageResponse<MessageDto> messages(Long conversationId, int page, int size) {
        return get("/conversations/" + conversationId + "/messages?page=" + page + "&size=" + size,
                new ParameterizedTypeReference<>() {});
    }

    public MessageDto sendMessage(Long conversationId, String content) {
        try {
            return client.post().uri("/conversations/" + conversationId + "/messages").headers(auth())
                    .body(new SendMessageRequest(conversationId, null, content)).retrieve().body(MessageDto.class);
        } catch (Exception ex) {
            throw translate(ex);
        }
    }

    public void markRead(Long conversationId) {
        try {
            client.post().uri("/conversations/" + conversationId + "/read").headers(auth()).retrieve().toBodilessEntity();
        } catch (Exception ex) {
            throw translate(ex);
        }
    }

    private <T> T get(String uri, ParameterizedTypeReference<T> type) {
        try {
            return client.get().uri(uri).headers(auth()).retrieve().body(type);
        } catch (Exception ex) {
            throw translate(ex);
        }
    }

    private Consumer<HttpHeaders> auth() {
        return headers -> {
            if (session.getToken() != null) {
                headers.setBearerAuth(session.getToken());
            }
        };
    }

    private RuntimeException translate(Exception ex) {
        if (ex instanceof RestClientResponseException responseException) {
            return new ApiException(extractMessage(responseException.getResponseBodyAsString()),
                    responseException.getStatusCode().value());
        }
        return new ApiException("No se pudo conectar con el servidor: " + ex.getMessage(), 0);
    }

    private String extractMessage(String body) {
        if (body == null || body.isBlank()) {
            return "Ocurrió un error inesperado";
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.hasNonNull("message")) {
                return node.get("message").asText();
            }
        } catch (Exception ignored) {
            // el cuerpo no es JSON, se devuelve tal cual
        }
        return body;
    }
}
