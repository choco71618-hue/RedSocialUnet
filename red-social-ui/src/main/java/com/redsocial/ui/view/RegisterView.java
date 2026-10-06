package com.redsocial.ui.view;

import com.redsocial.common.dto.AuthResponse;
import com.redsocial.common.dto.RegisterRequest;
import com.redsocial.ui.client.ApiException;
import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.session.UserSession;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("register")
@PageTitle("Crear cuenta | RED-SOCIAL")
public class RegisterView extends VerticalLayout implements BeforeEnterObserver {

    private final ApiService api;
    private final UserSession session;

    public RegisterView(ApiService api, UserSession session) {
        this.api = api;
        this.session = session;

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        H1 title = new H1("Crear cuenta");

        TextField displayName = new TextField("Nombre");
        displayName.setWidthFull();
        TextField username = new TextField("Usuario");
        username.setWidthFull();
        TextField email = new TextField("Email");
        email.setWidthFull();
        PasswordField password = new PasswordField("Contraseña");
        password.setWidthFull();

        Button register = new Button("Registrarme", event -> doRegister(
                displayName.getValue(), username.getValue(), email.getValue(), password.getValue()));
        register.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        register.setWidthFull();

        Button goLogin = new Button("Ya tengo cuenta", event -> UI.getCurrent().navigate(LoginView.class));
        goLogin.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        goLogin.setWidthFull();

        VerticalLayout card = new VerticalLayout(title, displayName, username, email, password, register, goLogin);
        card.setWidth("360px");
        add(card);
    }

    private void doRegister(String displayName, String username, String email, String password) {
        try {
            AuthResponse response = api.register(new RegisterRequest(username, email, password, displayName));
            session.setAuth(response);
            UI.getCurrent().navigate("");
        } catch (ApiException ex) {
            Notification.show(ex.getMessage());
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (session.isLoggedIn()) {
            event.forwardTo("");
        }
    }
}
