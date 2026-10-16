package com.redsocial.ui.component;

import com.redsocial.common.dto.CommentDto;
import com.redsocial.common.dto.PageResponse;
import com.redsocial.common.dto.PostDto;
import com.redsocial.ui.client.ApiException;
import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.session.UserSession;
import com.redsocial.ui.util.AvatarUtil;
import com.redsocial.ui.util.Toast;
import com.redsocial.ui.util.TimeFormat;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

public class CommentDialog extends Dialog {

    private final ApiService api;
    private final UserSession session;
    private final Runnable onChanged;

    public CommentDialog(PostDto post, ApiService api, UserSession session, Runnable onChanged) {
        this.api = api;
        this.session = session;
        this.onChanged = onChanged;
        setHeaderTitle("Comentarios");

        VerticalLayout list = new VerticalLayout();
        list.addClassName("rs-gap");

        TextArea input = new TextArea();
        input.setPlaceholder("Escribí un comentario...");
        input.setMaxLength(1000);

        Button send = new Button("Enviar", event -> {
            try {
                api.addComment(post.id(), input.getValue());
                input.clear();
                load(list, post, api);
                onChanged.run();
            } catch (ApiException ex) {
                Toast.show(ex.getMessage());
            }
        });
        send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout composer = new HorizontalLayout(AvatarUtil.of(session.getUser()), input, send);
        composer.addClassName("comment-composer");
        composer.addClassName("rs-items-end");
        input.addClassName("rs-grow");

        VerticalLayout content = new VerticalLayout(list, composer);
        content.addClassName("rs-gap");
        add(content);

        load(list, post, api);
    }

    private void load(VerticalLayout list, PostDto post, ApiService api) {
        list.removeAll();
        try {
            PageResponse<CommentDto> page = api.comments(post.id(), 0, 100);
            if (page.content().isEmpty()) {
                Span empty = new Span("Todavía no hay comentarios");
                empty.addClassName("muted");
                list.add(empty);
                return;
            }
            for (CommentDto comment : page.content()) {
                list.add(render(comment, post, list));
            }
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private HorizontalLayout render(CommentDto comment, PostDto post, VerticalLayout list) {
        Span author = new Span(display(comment));
        author.addClassName("post-author");
        Span body = new Span(comment.content());
        body.addClassName("comment-body");
        Span time = new Span(TimeFormat.relative(comment.createdAt()));
        time.addClassName("post-handle");

        HorizontalLayout meta = new HorizontalLayout(author, time);
        meta.addClassName("rs-gap-sm");
        meta.addClassName("rs-items-center");

        VerticalLayout text = new VerticalLayout(meta, body);

        HorizontalLayout row = new HorizontalLayout(
                AvatarUtil.create(comment.authorDisplayName(), comment.authorUsername(), comment.authorAvatarUrl()),
                text);
        row.addClassName("card");
        row.addClassName("rs-items-start");

        boolean canDelete = session.getUser() != null
                && (java.util.Objects.equals(comment.authorId(), session.getUser().id())
                || java.util.Objects.equals(post.authorId(), session.getUser().id()));
        if (canDelete) {
            Button delete = new Button(com.vaadin.flow.component.icon.VaadinIcon.TRASH.create(),
                    event -> deleteComment(comment, post, list));
            delete.addClassName("action-btn");
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
            row.add(delete);
            text.addClassName("rs-grow");
        }
        return row;
    }

    private void deleteComment(CommentDto comment, PostDto post, VerticalLayout list) {
        try {
            api.deleteComment(comment.id());
            load(list, post, api);
            onChanged.run();
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private String display(CommentDto comment) {
        return comment.authorDisplayName() == null || comment.authorDisplayName().isBlank()
                ? "@" + comment.authorUsername()
                : comment.authorDisplayName();
    }
}
