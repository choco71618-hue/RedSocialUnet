package com.redsocial.ui.view;

import com.redsocial.ui.session.UserSession;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class MainLayout extends AppLayout {

    public MainLayout(UserSession session) {
        setPrimarySection(Section.NAVBAR);

        Span brand = new Span("RED-SOCIAL");
        brand.addClassName("brand-title");

        Button feed = navButton("Inicio", "");
        Button explore = navButton("Explorar", "explore");
        Button chat = navButton("Chat", "chat");
        Button profile = navButton("Perfil", "profile");

        Span spacer = new Span();

        Button logout = new Button("Salir", event -> {
            session.logout();
            UI.getCurrent().navigate(LoginView.class);
        });
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

        HorizontalLayout navbar = new HorizontalLayout(brand, feed, explore, chat, profile, spacer, logout);
        navbar.setWidthFull();
        navbar.setAlignItems(FlexComponent.Alignment.CENTER);
        navbar.setFlexGrow(1, spacer);
        addToNavbar(navbar);
    }

    private Button navButton(String text, String route) {
        Button button = new Button(text, event -> UI.getCurrent().navigate(route));
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        return button;
    }
}
