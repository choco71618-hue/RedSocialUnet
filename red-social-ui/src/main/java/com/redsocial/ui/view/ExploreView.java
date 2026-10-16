package com.redsocial.ui.view;

import com.redsocial.common.dto.UserDto;
import com.redsocial.ui.client.ApiException;
import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.session.UserSession;
import com.redsocial.ui.util.AvatarUtil;
import com.redsocial.ui.util.Toast;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
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

        search.setPlaceholder("Buscar en el Edificio C...");
        search.setClearButtonVisible(true);

        Button searchButton = new Button("Buscar", VaadinIcon.SEARCH.create(), event -> doSearch());
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout bar = new HorizontalLayout(search, searchButton);
        bar.addClassName("explore-search-bar");
        bar.addClassName("rs-items-end");
        search.addClassName("rs-grow");

        add(header(), bar, results);
    }

    private Div header() {
        Div header = new Div();
        header.addClassName("page-header");
        Span eyebrow = new Span("Directorio");
        eyebrow.addClassName("eyebrow");
        Span title = new Span("Explorar Comunidad");
        title.addClassName("page-title");
        header.add(eyebrow, title);
        return header;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        super.beforeEnter(event);
        event.getLocation().getQueryParameters().getSingleParameter("q").ifPresent(query -> {
            search.setValue(query);
            doSearch();
        });
    }

    private void doSearch() {
        results.removeAll();
        try {
            List<UserDto> users = api.search(search.getValue() == null ? "" : search.getValue().trim());
            if (users.isEmpty()) {
                results.add(emptyState("Sin resultados", "Probá con otro nombre o usuario."));
                return;
            }
            users.forEach(this::renderUser);
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private void renderUser(UserDto user) {
        Span name = new Span(display(user));
        name.addClassName("post-author");
        name.addClickListener(event -> UI.getCurrent().navigate("profile/" + user.username()));
        Span handle = new Span("@" + user.username());
        handle.addClassName("post-handle");

        VerticalLayout info = new VerticalLayout(name, handle);

        Button action;
        boolean self = session.getUser() != null && Objects.equals(session.getUser().id(), user.id());
        if (self) {
            action = new Button("Ver perfil", event -> UI.getCurrent().navigate("profile"));
            action.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        } else if (user.following()) {
            action = new Button("Dejar de seguir", event -> toggle(user));
            action.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        } else {
            action = new Button("Seguir", event -> toggle(user));
            action.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }

        Div card = new Div();
        card.addClassName("user-card");
        card.addClassName("explore-user-row");
        card.add(AvatarUtil.of(user), info, action);
        results.add(card);
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
            Toast.show(ex.getMessage());
        }
    }

    private Div emptyState(String title, String subtitle) {
        Div div = new Div();
        div.addClassName("empty-state");
        Span icon = new Span(VaadinIcon.SEARCH.create());
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
