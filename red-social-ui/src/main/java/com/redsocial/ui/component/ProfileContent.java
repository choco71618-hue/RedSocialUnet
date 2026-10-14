package com.redsocial.ui.component;

import com.redsocial.common.dto.ConversationDto;
import com.redsocial.common.dto.MediaDto;
import com.redsocial.common.dto.PageResponse;
import com.redsocial.common.dto.PostDto;
import com.redsocial.common.dto.UpdateProfileRequest;
import com.redsocial.common.dto.UserDto;
import com.redsocial.ui.client.ApiException;
import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.session.UserSession;
import com.redsocial.ui.util.AvatarUtil;
import com.redsocial.ui.util.Toast;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

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
        body.setSpacing(true);
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
            body.add(buildTabs(user));
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private com.vaadin.flow.component.tabs.Tabs buildTabs(UserDto user) {
        com.vaadin.flow.component.tabs.Tab tabPosts = new com.vaadin.flow.component.tabs.Tab("Posts");
        com.vaadin.flow.component.tabs.Tab tabProjects = new com.vaadin.flow.component.tabs.Tab("Proyectos");
        com.vaadin.flow.component.tabs.Tab tabEvents = new com.vaadin.flow.component.tabs.Tab("Eventos");
        com.vaadin.flow.component.tabs.Tabs tabs = new com.vaadin.flow.component.tabs.Tabs(tabPosts, tabProjects, tabEvents);
        tabs.addClassName("profile-tabs");

        tabs.addSelectedChangeListener(event -> {
            body.removeAll();
            try {
                UserDto current = username == null ? api.me() : api.profile(username);
                body.add(buildHeader(current));
                body.add(buildTabs(current));
                com.vaadin.flow.component.tabs.Tab selected = event.getSelectedTab();
                if (selected == tabPosts) {
                    loadPosts(current);
                } else if (selected == tabProjects) {
                    loadProjects(current, com.redsocial.common.dto.ProjectKind.PROJECT);
                } else {
                    loadProjects(current, com.redsocial.common.dto.ProjectKind.EVENT);
                }
            } catch (ApiException ex) {
                Toast.show(ex.getMessage());
            }
        });
        loadPosts(user);
        return tabs;
    }

    private void loadPosts(UserDto user) {
        try {
            PageResponse<PostDto> posts = api.userPosts(user.username(), 0, 50);
            if (posts.content().isEmpty()) {
                body.add(emptyState("Sin publicaciones todavía",
                        "Cuando " + display(user) + " publique algo, aparecerá acá."));
                return;
            }
            posts.content().forEach(post -> body.add(new PostCard(post, api, session, this::refresh)));
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private void loadProjects(UserDto user, com.redsocial.common.dto.ProjectKind kind) {
        try {
            java.util.List<com.redsocial.common.dto.ProjectDto> projects =
                    api.projects(kind, null, user.username());
            if (projects.isEmpty()) {
                body.add(emptyState(kind == com.redsocial.common.dto.ProjectKind.EVENT
                        ? "Sin eventos cargados" : "Sin proyectos cargados",
                        "Todavía no hay nada acá."));
                return;
            }
            for (com.redsocial.common.dto.ProjectDto project : projects) {
                body.add(projectRow(project, kind));
            }
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private Div projectRow(com.redsocial.common.dto.ProjectDto project,
                           com.redsocial.common.dto.ProjectKind kind) {
        Div card = new Div();
        card.addClassName("project-card");
        Span title = new Span(project.title());
        title.addClassName("project-title");
        Span desc = new Span(project.description());
        desc.addClassName("project-description");
        card.add(title, desc);
        if (kind == com.redsocial.common.dto.ProjectKind.EVENT && project.eventDate() != null) {
            Span when = new Span(com.redsocial.ui.util.TimeFormat.relative(project.createdAt()));
            when.addClassName("project-meta");
            card.add(when);
        }
        return card;
    }

    private VerticalLayout buildHeader(UserDto user) {
        Div cover = new Div();
        cover.addClassName("profile-cover");
        if (user.coverUrl() != null && !user.coverUrl().isBlank()) {
            com.vaadin.flow.component.html.Image bg = new com.vaadin.flow.component.html.Image(user.coverUrl(), "portada");
            bg.addClassName("profile-cover-bg");
            cover.add(bg);
        }
        Avatar avatar = AvatarUtil.of(user);
        avatar.addClassName("rs-avatar-lg");
        avatar.addClassName("profile-avatar");
        cover.add(avatar);

        H2 name = new H2(display(user));
        name.addClassName("profile-name");
        Span handle = new Span("@" + user.username());
        handle.addClassName("profile-handle");

        HorizontalLayout stats = new HorizontalLayout(
                statPill(user.followersCount(), "seguidores"),
                statPill(user.followingCount(), "siguiendo"),
                statPill(user.postsCount(), "publicaciones"));
        stats.addClassName("profile-stats");
        stats.setPadding(false);
        stats.setSpacing(true);

        VerticalLayout info = new VerticalLayout();
        info.addClassName("profile-head");
        info.setPadding(false);
        info.setSpacing(false);
        info.add(name, handle);
        if (user.bio() != null && !user.bio().isBlank()) {
            Paragraph bio = new Paragraph(user.bio());
            bio.addClassName("profile-bio");
            info.add(bio);
        }
        info.add(stats, buildActions(user));

        VerticalLayout header = new VerticalLayout(cover, info);
        header.setPadding(false);
        header.setSpacing(true);
        return header;
    }

    private Span statPill(long value, String label) {
        Span span = new Span();
        span.addClassName("stat-pill");
        com.vaadin.flow.dom.Element number = new com.vaadin.flow.dom.Element("b");
        number.setText(String.valueOf(value));
        span.getElement().appendChild(number);
        span.add(new Span(" " + label));
        return span;
    }

    private HorizontalLayout buildActions(UserDto user) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.addClassName("profile-actions");
        actions.setPadding(false);
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
            message.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            actions.add(follow, message);
        }
        return actions;
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
            Toast.show(ex.getMessage());
        }
    }

    private void startChat(UserDto user) {
        try {
            ConversationDto conversation = api.startConversation(user.id());
            UI.getCurrent().navigate("chat?conversation=" + conversation.id());
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private void openEdit(UserDto user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Editar perfil");

        TextField displayName = new TextField("Nombre");
        displayName.setValue(user.displayName() == null ? "" : user.displayName());

        TextArea bio = new TextArea("Biografía");
        bio.setValue(user.bio() == null ? "" : user.bio());
        bio.setMaxLength(500);

        TextField avatarUrl = new TextField("URL del avatar (o subí una imagen)");
        avatarUrl.setValue(user.avatarUrl() == null ? "" : user.avatarUrl());

        MemoryBuffer avatarBuffer = new MemoryBuffer();
        Upload avatarUpload = new Upload(avatarBuffer);
        avatarUpload.setAcceptedFileTypes("image/png", "image/jpeg", "image/gif", "image/webp");
        avatarUpload.setMaxFileSize(8 * 1024 * 1024);
        avatarUpload.setDropLabel(new Span("Subir avatar"));
        avatarUpload.addSucceededListener(event -> {
            try {
                byte[] bytes = avatarBuffer.getInputStream().readAllBytes();
                MediaDto media = api.uploadMedia(bytes, event.getMIMEType(),
                        event.getFileName());
                avatarUrl.setValue(media.url());
            } catch (ApiException ex) {
                Toast.show(ex.getMessage());
            } catch (Exception ex) {
                Toast.show("No se pudo subir la imagen");
            }
        });

        TextField coverUrl = new TextField("URL del cover");
        coverUrl.setValue(user.coverUrl() == null ? "" : user.coverUrl());

        MemoryBuffer coverBuffer = new MemoryBuffer();
        Upload coverUpload = new Upload(coverBuffer);
        coverUpload.setAcceptedFileTypes("image/png", "image/jpeg", "image/gif", "image/webp");
        coverUpload.setMaxFileSize(8 * 1024 * 1024);
        coverUpload.setDropLabel(new Span("Subir cover"));
        coverUpload.addSucceededListener(event -> {
            try {
                byte[] bytes = coverBuffer.getInputStream().readAllBytes();
                MediaDto media = api.uploadMedia(bytes, event.getMIMEType(),
                        event.getFileName());
                coverUrl.setValue(media.url());
            } catch (ApiException ex) {
                Toast.show(ex.getMessage());
            } catch (Exception ex) {
                Toast.show("No se pudo subir la imagen");
            }
        });

        Button save = new Button("Guardar", event -> {
            try {
                UserDto updated = api.updateProfile(new UpdateProfileRequest(
                        displayName.getValue(), bio.getValue(), avatarUrl.getValue(), coverUrl.getValue()));
                session.setUser(updated);
                dialog.close();
                refresh();
            } catch (ApiException ex) {
                Toast.show(ex.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancelar", event -> dialog.close());

        VerticalLayout content = new VerticalLayout(displayName, bio, avatarUrl, avatarUpload, coverUrl, coverUpload);
        content.setPadding(false);
        dialog.add(content);
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private Div emptyState(String title, String subtitle) {
        Div div = new Div();
        div.addClassName("empty-state");
        Span icon = new Span(com.vaadin.flow.component.icon.VaadinIcon.ROCKET.create());
        icon.addClassName("empty-icon");
        Span titleSpan = new Span(title);
        titleSpan.addClassName("empty-state-title");
        div.add(icon, titleSpan, new Span(subtitle));
        return div;
    }

    private String display(UserDto user) {
        return user.displayName() == null || user.displayName().isBlank()
                ? user.username()
                : user.displayName();
    }
}
