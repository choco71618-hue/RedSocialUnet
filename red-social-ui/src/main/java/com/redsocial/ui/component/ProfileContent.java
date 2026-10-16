package com.redsocial.ui.component;

import com.redsocial.common.dto.ConversationDto;
import com.redsocial.common.dto.FriendRequestDto;
import com.redsocial.common.dto.MediaDto;
import com.redsocial.common.dto.PageResponse;
import com.redsocial.common.dto.PostDto;
import com.redsocial.common.dto.TutorType;
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
import com.vaadin.flow.component.select.Select;
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
    private final VerticalLayout tabContent = new VerticalLayout();

    public ProfileContent(String username, ApiService api, UserSession session) {
        this.username = username;
        this.api = api;
        this.session = session;
        addClassName("rs-gap");
        body.addClassName("rs-gap");
        tabContent.addClassName("rs-gap");
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
            tabContent.removeAll();
            body.add(buildHeader(user), buildTabs(user), tabContent);
            loadPosts(user);
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
            UserDto current;
            try {
                current = username == null ? api.me() : api.profile(username);
            } catch (ApiException ex) {
                Toast.show(ex.getMessage());
                return;
            }
            com.vaadin.flow.component.tabs.Tab selected = event.getSelectedTab();
            if (selected == tabProjects) {
                loadProjects(current, com.redsocial.common.dto.ProjectKind.PROJECT);
            } else if (selected == tabEvents) {
                loadProjects(current, com.redsocial.common.dto.ProjectKind.EVENT);
            } else {
                loadPosts(current);
            }
        });
        return tabs;
    }

    private void loadPosts(UserDto user) {
        tabContent.removeAll();
        try {
            PageResponse<PostDto> posts = api.userPosts(user.username(), 0, 50);
            if (posts.content().isEmpty()) {
                tabContent.add(emptyState("Sin publicaciones todavía",
                        "Cuando " + display(user) + " publique algo, aparecerá acá."));
                return;
            }
            posts.content().forEach(post -> tabContent.add(new PostCard(post, api, session, this::refresh)));
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private void loadProjects(UserDto user, com.redsocial.common.dto.ProjectKind kind) {
        tabContent.removeAll();
        try {
            java.util.List<com.redsocial.common.dto.ProjectDto> projects =
                    api.projects(kind, null, user.username());
            if (projects.isEmpty()) {
                tabContent.add(emptyState(kind == com.redsocial.common.dto.ProjectKind.EVENT
                        ? "Sin eventos cargados" : "Sin proyectos cargados",
                        "Todavía no hay nada acá."));
                return;
            }
            for (com.redsocial.common.dto.ProjectDto project : projects) {
                tabContent.add(projectRow(project, kind));
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
                statPill(user.followersCount(), "seguidores", () -> openFollowList(user, true)),
                statPill(user.followingCount(), "siguiendo", () -> openFollowList(user, false)),
                statPill(user.postsCount(), "publicaciones", null));
        boolean selfProfile = username == null
                || (session.getUser() != null && Objects.equals(session.getUser().id(), user.id()));
        if (selfProfile) {
            stats.add(statPill(safeFriendsCount(), "amigos", this::openFriendsDialog));
        } else {
            stats.add(statPill(safeMutual(user.username()), "en común", null));
        }
        stats.addClassName("profile-stats");

        VerticalLayout info = new VerticalLayout();
        info.addClassName("profile-head");
        info.add(name, handle);
        if (user.bio() != null && !user.bio().isBlank()) {
            Paragraph bio = new Paragraph(user.bio());
            bio.addClassName("profile-bio");
            info.add(bio);
        }
        HorizontalLayout academic = academicChips(user);
        if (academic != null) {
            info.add(academic);
        }
        info.add(stats, buildActions(user));

        VerticalLayout header = new VerticalLayout(cover, info);
        header.addClassName("rs-gap");
        return header;
    }

    private Span statPill(long value, String label, Runnable onClick) {
        Span span = new Span();
        span.addClassName("stat-pill");
        com.vaadin.flow.dom.Element number = new com.vaadin.flow.dom.Element("b");
        number.setText(String.valueOf(value));
        span.getElement().appendChild(number);
        span.add(new Span(" " + label));
        if (onClick != null) {
            span.addClassName("clickable");
            span.addClickListener(event -> onClick.run());
        }
        return span;
    }

    private void openFollowList(UserDto profile, boolean followers) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(followers ? "Seguidores" : "Siguiendo");
        VerticalLayout list = new VerticalLayout();
        list.addClassName("rs-gap-sm");
        dialog.add(list);
        renderFollowList(dialog, list, profile, followers);
        dialog.open();
    }

    private void renderFollowList(Dialog dialog, VerticalLayout list, UserDto profile, boolean followers) {
        list.removeAll();
        try {
            java.util.List<UserDto> users = followers
                    ? api.followers(profile.username())
                    : api.following(profile.username());
            if (users.isEmpty()) {
                list.add(emptyState(followers ? "Sin seguidores todavía" : "No sigue a nadie todavía",
                        "Cuando haya novedades, aparecerán acá."));
                return;
            }
            for (UserDto user : users) {
                list.add(followRow(dialog, list, profile, followers, user));
            }
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private Div followRow(Dialog dialog, VerticalLayout list, UserDto profile, boolean followers, UserDto user) {
        Avatar avatar = AvatarUtil.of(user);
        avatar.addClassName("rs-avatar-sm");

        Span name = new Span(display(user));
        name.addClassName("post-author");
        name.addClickListener(event -> {
            dialog.close();
            UI.getCurrent().navigate("profile/" + user.username());
        });
        Span handle = new Span("@" + user.username());
        handle.addClassName("post-handle");

        VerticalLayout info = new VerticalLayout(name, handle);

        Div card = new Div();
        card.addClassName("user-card");
        card.addClassName("explore-user-row");

        boolean self = session.getUser() != null && Objects.equals(session.getUser().id(), user.id());
        Button action;
        if (self) {
            action = new Button("Ver perfil", event -> {
                dialog.close();
                UI.getCurrent().navigate("profile");
            });
            action.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        } else if (user.following()) {
            action = new Button("Dejar de seguir",
                    event -> toggleFromList(dialog, list, profile, followers, user));
            action.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        } else {
            action = new Button("Seguir",
                    event -> toggleFromList(dialog, list, profile, followers, user));
            action.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }

        card.add(avatar, info, action);
        return card;
    }

    private void toggleFromList(Dialog dialog, VerticalLayout list, UserDto profile, boolean followers, UserDto user) {
        try {
            if (user.following()) {
                api.unfollow(user.username());
            } else {
                api.follow(user.username());
            }
            renderFollowList(dialog, list, profile, followers);
            refresh();
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private HorizontalLayout academicChips(UserDto user) {
        HorizontalLayout row = new HorizontalLayout();
        row.addClassName("profile-academic");
        boolean any = chip(row, user.carnet() == null || user.carnet().isBlank()
                ? null : ("Carnet " + user.carnet()), "academic-chip");
        if ("STUDENT".equals(user.role())) {
            any = chip(row, "Estudiante", "role-student") || any;
            any = chip(row, user.career(), "academic-chip") || any;
            any = chip(row, user.subjectOfInterest(), "academic-chip") || any;
        } else if ("TUTOR".equals(user.role())) {
            any = chip(row, "Preparador", "role-tutor") || any;
            String type = user.tutorType() == null
                    ? null
                    : (user.tutorType() == TutorType.UNET ? "Preparador UNET" : "Clases privadas");
            any = chip(row, type, "academic-chip") || any;
        }
        return any ? row : null;
    }

    private boolean chip(HorizontalLayout row, String text, String className) {
        if (text == null || text.isBlank()) {
            return false;
        }
        Span span = new Span(text);
        span.addClassName("profile-chip");
        span.addClassName(className);
        row.add(span);
        return true;
    }

    private HorizontalLayout buildActions(UserDto user) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.addClassName("profile-actions");
        boolean self = username == null || Objects.equals(session.getUser().id(), user.id());
        if (self) {
            Button edit = new Button("Editar perfil", event -> openEdit(user));
            edit.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

            Button logout = new Button("Salir",
                    com.vaadin.flow.component.icon.VaadinIcon.SIGN_OUT.create(), event -> {
                        session.logout();
                        UI.getCurrent().navigate(com.redsocial.ui.view.LoginView.class);
                    });
            logout.addClassName("profile-logout");
            logout.addThemeVariants(ButtonVariant.LUMO_ERROR);

            actions.add(edit, logout);
        } else {
            Button friend = friendButton(user);
            Button follow = new Button(user.following() ? "Dejar de seguir" : "Seguir",
                    event -> toggleFollow(user));
            if (!user.following()) {
                follow.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            }
            Button message = new Button("Mensaje", event -> startChat(user));
            message.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            actions.add(friend, follow, message);
        }
        return actions;
    }

    private Button friendButton(UserDto user) {
        String status;
        try {
            status = api.friendStatus(user.username());
        } catch (ApiException ex) {
            status = "NONE";
        }
        Button button = switch (status == null ? "NONE" : status) {
            case "ACCEPTED" -> new Button("Amigos",
                    com.vaadin.flow.component.icon.VaadinIcon.USERS.create(),
                    event -> removeFriend(user.username()));
            case "PENDING_OUT" -> new Button("Solicitud enviada",
                    event -> cancelOutgoing(user.username()));
            case "PENDING_IN" -> new Button("Aceptar solicitud",
                    com.vaadin.flow.component.icon.VaadinIcon.CHECK.create(),
                    event -> acceptIncoming(user.username()));
            default -> new Button("Añadir amigo",
                    com.vaadin.flow.component.icon.VaadinIcon.USER.create(),
                    event -> sendFriendRequest(user.username()));
        };
        switch (status == null ? "NONE" : status) {
            case "ACCEPTED", "PENDING_OUT" -> button.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            case "PENDING_IN" -> button.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            default -> button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }
        button.addClassName("profile-friend");
        return button;
    }

    private void sendFriendRequest(String username) {
        try {
            api.sendFriendRequest(username);
            Toast.show("Solicitud de amistad enviada");
            refresh();
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private void removeFriend(String username) {
        try {
            api.removeFriend(username);
            Toast.show("Amistad eliminada");
            refresh();
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private void acceptIncoming(String username) {
        try {
            for (FriendRequestDto request : api.friendRequests()) {
                if (request.user() != null && username.equalsIgnoreCase(request.user().username())) {
                    api.acceptFriendRequest(request.id());
                    break;
                }
            }
            Toast.show("¡Ahora son amigos!");
            refresh();
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private void cancelOutgoing(String username) {
        try {
            for (FriendRequestDto request : api.sentFriendRequests()) {
                if (request.user() != null && username.equalsIgnoreCase(request.user().username())) {
                    api.cancelFriendRequest(request.id());
                    break;
                }
            }
            Toast.show("Solicitud cancelada");
            refresh();
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private long safeFriendsCount() {
        try {
            return api.friends().size();
        } catch (ApiException ex) {
            return 0;
        }
    }

    private long safeMutual(String username) {
        try {
            return api.mutualFriends(username);
        } catch (ApiException ex) {
            return 0;
        }
    }

    private void openFriendsDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Amigos");
        VerticalLayout list = new VerticalLayout();
        list.addClassName("rs-gap-sm");
        try {
            java.util.List<UserDto> friends = api.friends();
            if (friends.isEmpty()) {
                list.add(emptyState("Sin amigos todavía", "Enviá solicitudes desde el perfil de otros usuarios."));
            } else {
                for (UserDto friend : friends) {
                    list.add(friendRow(dialog, friend));
                }
            }
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
        dialog.add(list);
        dialog.open();
    }

    private Div friendRow(Dialog dialog, UserDto user) {
        Avatar avatar = AvatarUtil.of(user);
        avatar.addClassName("rs-avatar-sm");

        Span name = new Span(display(user));
        name.addClassName("post-author");
        name.addClickListener(event -> {
            dialog.close();
            UI.getCurrent().navigate("profile/" + user.username());
        });
        Span handle = new Span("@" + user.username());
        handle.addClassName("post-handle");

        VerticalLayout info = new VerticalLayout(name, handle);

        Div card = new Div();
        card.addClassName("user-card");
        card.addClassName("explore-user-row");
        card.add(avatar, info);
        return card;
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

        TextField career = new TextField("Carrera");
        career.setValue(user.career() == null ? "" : user.career());
        career.setVisible("STUDENT".equals(user.role()));

        TextField interest = new TextField("Materia de interés");
        interest.setValue(user.subjectOfInterest() == null ? "" : user.subjectOfInterest());
        interest.setVisible("STUDENT".equals(user.role()));

        Select<TutorType> tutorType = new Select<>();
        tutorType.setLabel("Tipo de preparador");
        tutorType.setItems(TutorType.values());
        tutorType.setItemLabelGenerator(t -> t == TutorType.UNET ? "Preparador UNET" : "Clases privadas");
        tutorType.setValue(user.tutorType() == null ? TutorType.UNET : user.tutorType());
        tutorType.setVisible("TUTOR".equals(user.role()));

        Button save = new Button("Guardar", event -> {
            try {
                UserDto updated = api.updateProfile(new UpdateProfileRequest(
                        displayName.getValue(), bio.getValue(), avatarUrl.getValue(), coverUrl.getValue(),
                        career.getValue(), interest.getValue(),
                        tutorType.isVisible() ? tutorType.getValue() : null));
                session.setUser(updated);
                dialog.close();
                refresh();
            } catch (ApiException ex) {
                Toast.show(ex.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancelar", event -> dialog.close());

        VerticalLayout content = new VerticalLayout(displayName, bio, avatarUrl, avatarUpload,
                coverUrl, coverUpload, career, interest, tutorType);
        content.addClassName("rs-gap");
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
