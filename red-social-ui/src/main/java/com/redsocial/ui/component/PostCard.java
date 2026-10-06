package com.redsocial.ui.component;

import com.redsocial.common.dto.PostDto;
import com.redsocial.ui.client.ApiException;
import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.session.UserSession;
import com.redsocial.ui.util.TimeFormat;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.Objects;

public class PostCard extends VerticalLayout {

    private final PostDto post;
    private final ApiService api;
    private final UserSession session;
    private final Runnable onChanged;

    public PostCard(PostDto post, ApiService api, UserSession session, Runnable onChanged) {
        this.post = post;
        this.api = api;
        this.session = session;
        this.onChanged = onChanged;

        addClassName("post-card");
        setPadding(false);
        setSpacing(false);

        Span author = new Span(displayName());
        author.addClassName("post-author");
        author.getStyle().set("cursor", "pointer");
        author.addClickListener(event -> UI.getCurrent().navigate("profile/" + post.authorUsername()));

        Span meta = new Span("@" + post.authorUsername() + " · " + TimeFormat.relative(post.createdAt()));
        meta.addClassName("post-meta");

        Paragraph content = new Paragraph(post.content());
        content.getStyle().set("white-space", "pre-wrap");
        content.getStyle().set("margin", "0.5rem 0");

        HorizontalLayout actions = buildActions();

        add(new VerticalLayout(author, meta), content, actions);
    }

    private HorizontalLayout buildActions() {
        Button like = new Button((post.likedByMe() ? "♥ " : "♡ ") + post.likeCount(), event -> toggleLike());
        like.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        Button comment = new Button("Comentar (" + post.commentCount() + ")",
                event -> new CommentDialog(post, api, session, onChanged).open());
        comment.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        HorizontalLayout actions = new HorizontalLayout(like, comment);

        boolean mine = session.getUser() != null && Objects.equals(post.authorId(), session.getUser().id());
        if (mine) {
            Button delete = new Button("Eliminar", event -> deletePost());
            delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            actions.add(delete);
        }
        return actions;
    }

    private void toggleLike() {
        try {
            if (post.likedByMe()) {
                api.unlike(post.id());
            } else {
                api.like(post.id());
            }
            onChanged.run();
        } catch (ApiException ex) {
            Notification.show(ex.getMessage());
        }
    }

    private void deletePost() {
        try {
            api.deletePost(post.id());
            onChanged.run();
        } catch (ApiException ex) {
            Notification.show(ex.getMessage());
        }
    }

    private String displayName() {
        return post.authorDisplayName() == null || post.authorDisplayName().isBlank()
                ? post.authorUsername()
                : post.authorDisplayName();
    }
}
