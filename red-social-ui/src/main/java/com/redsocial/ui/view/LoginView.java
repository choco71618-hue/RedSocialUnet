package com.redsocial.ui.view;

import com.redsocial.common.dto.AuthResponse;
import com.redsocial.common.dto.LoginRequest;
import com.redsocial.ui.client.ApiException;
import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.session.UserSession;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("login")
@PageTitle("Ingresar | RED-SOCIAL")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final ApiService api;
    private final UserSession session;

    public LoginView(ApiService api, UserSession session) {
        this.api = api;
        this.session = session;

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        H1 title = new H1("RED-SOCIAL");
        Paragraph subtitle = new Paragraph("Ingresá para ver tu feed");

        TextField username = new TextField("Usuario o email");
        username.setWidthFull();
        PasswordField password = new PasswordField("Contraseña");
        password.setWidthFull();

        Button login = new Button("Ingresar", event -> doLogin(username.getValue(), password.getValue()));
        login.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        login.setWidthFull();

        Button goRegister = new Button("Crear una cuenta", event -> UI.getCurrent().navigate(RegisterView.class));
        goRegister.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        goRegister.setWidthFull();

        VerticalLayout card = new VerticalLayout(title, subtitle, username, password, login, goRegister);
        card.setWidth("360px");
        card.setSpacing(true);
        add(card);
    }

    private void doLogin(String identifier, String secret) {
        try {
            AuthResponse response = api.login(new LoginRequest(identifier, secret));
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
