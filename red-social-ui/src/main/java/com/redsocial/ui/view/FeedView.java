package com.redsocial.ui.view;

import com.redsocial.common.dto.PageResponse;
import com.redsocial.common.dto.PostDto;
import com.redsocial.ui.client.ApiException;
import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.component.PostCard;
import com.redsocial.ui.session.UserSession;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Inicio | RED-SOCIAL")
public class FeedView extends ProtectedView {

    private final ApiService api;
    private final TextArea composer = new TextArea();
    private final VerticalLayout posts = new VerticalLayout();

    public FeedView(ApiService api, UserSession session) {
        super(session);
        this.api = api;

        composer.setPlaceholder("¿Qué está pasando?");
        composer.setWidthFull();
        composer.setMaxLength(2000);
        composer.setMinHeight("80px");

        Button publish = new Button("Publicar", event -> publish());
        publish.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout row = new HorizontalLayout(composer, publish);
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.END);
        row.setFlexGrow(1, composer);

        VerticalLayout composerCard = new VerticalLayout(row);
        composerCard.addClassName("post-card");
        composerCard.setPadding(false);

        posts.setPadding(false);

        add(composerCard, new H3("Tu feed"), posts);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        super.beforeEnter(event);
        if (session.isLoggedIn()) {
            load();
        }
    }

    private void publish() {
        try {
            api.createPost(composer.getValue());
            composer.clear();
            load();
        } catch (ApiException ex) {
            Notification.show(ex.getMessage());
        }
    }

    private void load() {
        posts.removeAll();
        try {
            PageResponse<PostDto> page = api.feed(0, 50);
            if (page.content().isEmpty()) {
                posts.add(new com.vaadin.flow.component.html.Span(
                        "Tu feed está vacío. Seguí a otros usuarios o publicá algo."));
                return;
            }
            page.content().forEach(post -> posts.add(new PostCard(post, api, session, this::load)));
        } catch (ApiException ex) {
            Notification.show(ex.getMessage());
        }
    }
}
