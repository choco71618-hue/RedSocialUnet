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
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

public class CommentDialog extends Dialog {

    public CommentDialog(PostDto post, ApiService api, UserSession session, Runnable onChanged) {
        setHeaderTitle("Comentarios");
        setWidth("580px");

        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(true);

        TextArea input = new TextArea();
        input.setPlaceholder("Escribí un comentario...");
        input.setWidthFull();
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
        composer.setWidthFull();
        composer.setAlignItems(FlexComponent.Alignment.END);
        composer.setFlexGrow(1, input);

        VerticalLayout content = new VerticalLayout(list, composer);
        content.setPadding(false);
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
                list.add(render(comment));
            }
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private HorizontalLayout render(CommentDto comment) {
        Span author = new Span(display(comment));
        author.addClassName("post-author");
        Span body = new Span(comment.content());
        body.addClassName("comment-body");
        Span time = new Span(TimeFormat.relative(comment.createdAt()));
        time.addClassName("post-handle");

        HorizontalLayout meta = new HorizontalLayout(author, time);
        meta.setPadding(false);
        meta.setSpacing(true);
        meta.setAlignItems(FlexComponent.Alignment.CENTER);

        VerticalLayout text = new VerticalLayout(meta, body);
        text.setPadding(false);
        text.setSpacing(false);

        HorizontalLayout row = new HorizontalLayout(
                AvatarUtil.create(comment.authorDisplayName(), comment.authorUsername(), comment.authorAvatarUrl()),
                text);
        row.addClassName("card");
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.START);
        return row;
    }

    private String display(CommentDto comment) {
        return comment.authorDisplayName() == null || comment.authorDisplayName().isBlank()
                ? "@" + comment.authorUsername()
                : comment.authorDisplayName();
    }
}
