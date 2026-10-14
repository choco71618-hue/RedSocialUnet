package com.redsocial.ui.component;

import com.redsocial.common.dto.PostDto;
import com.redsocial.ui.client.ApiException;
import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.session.UserSession;
import com.redsocial.ui.util.AvatarUtil;
import com.redsocial.ui.util.Toast;
import com.redsocial.ui.util.TimeFormat;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
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

        add(buildHeader(), buildContent(), buildActions());
    }

    private HorizontalLayout buildHeader() {
        Avatar avatar = AvatarUtil.create(post.authorDisplayName(), post.authorUsername(), post.authorAvatarUrl());
        avatar.addClassName("rs-avatar-sm");

        Span author = new Span(displayName());
        author.addClassName("post-author");
        author.addClickListener(event -> UI.getCurrent().navigate("profile/" + post.authorUsername()));

        Span handle = new Span("@" + post.authorUsername());
        handle.addClassName("post-handle");
        Span separator = new Span("·");
        separator.addClassName("post-handle");
        Span time = new Span(TimeFormat.relative(post.createdAt()));
        time.addClassName("post-handle");

        HorizontalLayout meta = new HorizontalLayout(author, handle, separator, time);
        meta.setPadding(false);
        meta.setSpacing(true);
        meta.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout header = new HorizontalLayout(avatar, meta);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        return header;
    }

    private VerticalLayout buildContent() {
        VerticalLayout contentBlock = new VerticalLayout();
        contentBlock.setPadding(false);
        contentBlock.setSpacing(true);

        Paragraph content = new Paragraph(post.content());
        content.addClassName("post-content");
        contentBlock.add(content);

        if (post.mediaUrl() != null && !post.mediaUrl().isBlank()) {
            com.vaadin.flow.component.html.Image image = new com.vaadin.flow.component.html.Image(post.mediaUrl(), "imagen");
            image.addClassName("post-media");
            image.getElement().setProperty("data-loaded", "false");
            image.getElement().addEventListener("load", e ->
                    image.getElement().setProperty("data-loaded", "true"));
            image.addClickListener(event -> openLightbox(post.mediaUrl()));
            contentBlock.add(image);
        }
        return contentBlock;
    }

    private void openLightbox(String src) {
        com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();
        dialog.getElement().setAttribute("theme", "no-padding");
        var img = new com.vaadin.flow.component.html.Image(src, "imagen completa");
        img.addClassName("lightbox-img");
        dialog.add(img);
        dialog.setCloseOnOutsideClick(true);
        dialog.open();
    }

    private HorizontalLayout buildActions() {
        Button like = new Button(String.valueOf(post.likeCount()), VaadinIcon.HEART.create(), event -> toggleLike());
        like.addClassName("action-btn");
        if (post.likedByMe()) {
            like.addClassName("liked");
        }

        Button comment = new Button(String.valueOf(post.commentCount()), VaadinIcon.COMMENT.create(),
                event -> new CommentDialog(post, api, session, onChanged).open());
        comment.addClassName("action-btn");

        HorizontalLayout actions = new HorizontalLayout(like, comment);
        actions.addClassName("post-actions");
        actions.setPadding(false);
        actions.setSpacing(true);

        boolean mine = session.getUser() != null && Objects.equals(post.authorId(), session.getUser().id());
        if (mine) {
            Button delete = new Button(VaadinIcon.TRASH.create(), event -> deletePost());
            delete.addClassName("action-btn");
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
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
            Toast.show(ex.getMessage());
        }
    }

    private void deletePost() {
        try {
            api.deletePost(post.id());
            onChanged.run();
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private String displayName() {
        return post.authorDisplayName() == null || post.authorDisplayName().isBlank()
                ? post.authorUsername()
                : post.authorDisplayName();
    }
}
