package com.redsocial.ui.view;

import com.redsocial.common.dto.UserDto;
import com.redsocial.ui.client.ApiException;
import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.session.UserSession;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Objects;

@Route(value = "explore", layout = MainLayout.class)
@PageTitle("Explorar | RED-SOCIAL")
public class ExploreView extends ProtectedView {

    private final ApiService api;
    private final TextField search = new TextField();
    private final VerticalLayout results = new VerticalLayout();

    public ExploreView(ApiService api, UserSession session) {
        super(session);
        this.api = api;

        search.setPlaceholder("Buscar usuarios...");
        search.setWidthFull();
        search.setClearButtonVisible(true);

        Button searchButton = new Button("Buscar", event -> doSearch());
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout bar = new HorizontalLayout(search, searchButton);
        bar.setWidthFull();
        bar.setAlignItems(FlexComponent.Alignment.END);
        bar.setFlexGrow(1, search);

        results.setPadding(false);

        add(new H3("Explorar"), bar, results);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        super.beforeEnter(event);
    }

    private void doSearch() {
        results.removeAll();
        try {
            List<UserDto> users = api.search(search.getValue() == null ? "" : search.getValue().trim());
            if (users.isEmpty()) {
                results.add(new Span("Sin resultados"));
                return;
            }
            users.forEach(this::renderUser);
        } catch (ApiException ex) {
            Notification.show(ex.getMessage());
        }
    }

    private void renderUser(UserDto user) {
        Span name = new Span(display(user));
        name.addClassName("post-author");
        name.getStyle().set("cursor", "pointer");
        name.addClickListener(event -> UI.getCurrent().navigate("profile/" + user.username()));

        Span handle = new Span("@" + user.username());
        handle.addClassName("muted");

        VerticalLayout info = new VerticalLayout(name, handle);
        info.setPadding(false);
        info.setSpacing(false);

        Button action;
        boolean self = session.getUser() != null && Objects.equals(session.getUser().id(), user.id());
        if (self) {
            action = new Button("Ver perfil", event -> UI.getCurrent().navigate("profile"));
        } else if (user.following()) {
            action = new Button("Dejar de seguir", event -> toggle(user));
        } else {
            action = new Button("Seguir", event -> toggle(user));
            action.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }

        Span spacer = new Span();
        HorizontalLayout row = new HorizontalLayout(info, spacer, action);
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setFlexGrow(1, spacer);
        row.addClassName("post-card");
        results.add(row);
    }

    private void toggle(UserDto user) {
        try {
            if (user.following()) {
                api.unfollow(user.username());
            } else {
                api.follow(user.username());
            }
            doSearch();
        } catch (ApiException ex) {
            Notification.show(ex.getMessage());
        }
    }

    private String display(UserDto user) {
        return user.displayName() == null || user.displayName().isBlank()
                ? user.username()
                : user.displayName();
    }
}
