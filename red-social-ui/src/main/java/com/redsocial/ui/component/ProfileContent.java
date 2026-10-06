package com.redsocial.ui.component;

import com.redsocial.common.dto.ConversationDto;
import com.redsocial.common.dto.PageResponse;
import com.redsocial.common.dto.PostDto;
import com.redsocial.common.dto.UpdateProfileRequest;
import com.redsocial.common.dto.UserDto;
import com.redsocial.ui.client.ApiException;
import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.session.UserSession;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.util.Objects;

public class ProfileContent extends VerticalLayout {

    private final String username;
    private final ApiService api;
    private final UserSession session;
    private final VerticalLayout body = new VerticalLayout();

    public ProfileContent(String username, ApiService api, UserSession session) {
        this.username = username;
        this.api = api;
        this.session = session;
        setPadding(false);
        setSpacing(true);
        body.setPadding(false);
        add(body);
        refresh();
    }

    public void refresh() {
        body.removeAll();
        try {
            UserDto user = username == null ? api.me() : api.profile(username);
            if (username == null) {
                session.setUser(user);
            }
            body.add(buildHeader(user));
            body.add(new H3("Publicaciones"));
            PageResponse<PostDto> posts = api.userPosts(user.username(), 0, 50);
            if (posts.content().isEmpty()) {
                body.add(new Span("Todavía no hay publicaciones"));
            }
            posts.content().forEach(post -> body.add(new PostCard(post, api, session, this::refresh)));
        } catch (ApiException ex) {
            Notification.show(ex.getMessage());
        }
    }

    private VerticalLayout buildHeader(UserDto user) {
        H2 name = new H2(display(user));

        HorizontalLayout stats = new HorizontalLayout(
                stat(user.followersCount(), "seguidores"),
                stat(user.followingCount(), "siguiendo"),
                stat(user.postsCount(), "publicaciones"));

        VerticalLayout header = new VerticalLayout(name, new Span("@" + user.username()), stats, buildActions(user));
        if (user.bio() != null && !user.bio().isBlank()) {
            Paragraph bio = new Paragraph(user.bio());
            bio.getStyle().set("white-space", "pre-wrap");
            header.addComponentAtIndex(2, bio);
        }
        header.setPadding(false);
        header.setSpacing(false);
        return header;
    }

    private HorizontalLayout buildActions(UserDto user) {
        HorizontalLayout actions = new HorizontalLayout();
        boolean self = username == null || Objects.equals(session.getUser().id(), user.id());
        if (self) {
            Button edit = new Button("Editar perfil", event -> openEdit(user));
            edit.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            actions.add(edit);
        } else {
            Button follow = new Button(user.following() ? "Dejar de seguir" : "Seguir",
                    event -> toggleFollow(user));
            if (!user.following()) {
                follow.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            }
            Button message = new Button("Mensaje", event -> startChat(user));
            actions.add(follow, message);
        }
        actions.setPadding(false);
        return actions;
    }

    private Span stat(long value, String label) {
        Span span = new Span(value + " " + label);
        span.getStyle().set("margin-right", "1rem");
        span.addClassName("muted");
        return span;
    }

    private void toggleFollow(UserDto user) {
        try {
            if (user.following()) {
                api.unfollow(user.username());
            } else {
                api.follow(user.username());
            }
            refresh();
        } catch (ApiException ex) {
            Notification.show(ex.getMessage());
        }
    }

    private void startChat(UserDto user) {
        try {
            ConversationDto conversation = api.startConversation(user.id());
            UI.getCurrent().navigate("chat?conversation=" + conversation.id());
        } catch (ApiException ex) {
            Notification.show(ex.getMessage());
        }
    }

    private void openEdit(UserDto user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Editar perfil");

        TextField displayName = new TextField("Nombre");
        displayName.setValue(user.displayName() == null ? "" : user.displayName());
        displayName.setWidthFull();

        TextArea bio = new TextArea("Biografía");
        bio.setValue(user.bio() == null ? "" : user.bio());
        bio.setWidthFull();
        bio.setMaxLength(500);

        TextField avatarUrl = new TextField("URL del avatar");
        avatarUrl.setValue(user.avatarUrl() == null ? "" : user.avatarUrl());
        avatarUrl.setWidthFull();

        Button save = new Button("Guardar", event -> {
            try {
                UserDto updated = api.updateProfile(
                        new UpdateProfileRequest(displayName.getValue(), bio.getValue(), avatarUrl.getValue()));
                session.setUser(updated);
                dialog.close();
                refresh();
            } catch (ApiException ex) {
                Notification.show(ex.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancelar", event -> dialog.close());

        VerticalLayout content = new VerticalLayout(displayName, bio, avatarUrl);
        content.setPadding(false);
        dialog.add(content);
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private String display(UserDto user) {
        return user.displayName() == null || user.displayName().isBlank()
                ? user.username()
                : user.displayName();
    }
}
