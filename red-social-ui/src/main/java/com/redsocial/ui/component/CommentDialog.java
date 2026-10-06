package com.redsocial.ui.component;

import com.redsocial.common.dto.CommentDto;
import com.redsocial.common.dto.PageResponse;
import com.redsocial.common.dto.PostDto;
import com.redsocial.ui.client.ApiException;
import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.session.UserSession;
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
        setWidth("560px");

        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(false);

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
                Notification.show(ex.getMessage());
            }
        });
        send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout composer = new HorizontalLayout(input, send);
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
                list.add(new Span("Todavía no hay comentarios"));
                return;
            }
            for (CommentDto comment : page.content()) {
                Span author = new Span(displayName(comment));
                author.addClassName("post-author");
                Span body = new Span(comment.content());
                body.getStyle().set("white-space", "pre-wrap");
                VerticalLayout item = new VerticalLayout(author, body);
                item.setPadding(false);
                item.setSpacing(false);
                list.add(item);
            }
        } catch (ApiException ex) {
            Notification.show(ex.getMessage());
        }
    }

    private String displayName(CommentDto comment) {
        return comment.authorDisplayName() == null || comment.authorDisplayName().isBlank()
                ? "@" + comment.authorUsername()
                : comment.authorDisplayName();
    }
}
