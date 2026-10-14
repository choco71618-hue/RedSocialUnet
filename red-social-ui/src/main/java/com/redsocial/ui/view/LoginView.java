package com.redsocial.ui.view;

import com.redsocial.common.dto.AuthResponse;
import com.redsocial.common.dto.LoginRequest;
import com.redsocial.ui.client.ApiException;
import com.redsocial.ui.util.Toast;
import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.component.LottieView;
import com.redsocial.ui.session.UserSession;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("login")
@PageTitle("Ingresar | RED-SOCIAL")
public class LoginView extends HorizontalLayout implements BeforeEnterObserver {

    private final ApiService api;
    private final UserSession session;

    public LoginView(ApiService api, UserSession session) {
        this.api = api;
        this.session = session;

        addClassName("auth-split");
        setPadding(false);
        setSpacing(false);

        VerticalLayout hero = hero();
        VerticalLayout panel = panel();
        add(hero, panel);
    }

    private VerticalLayout hero() {
        VerticalLayout hero = new VerticalLayout();
        hero.addClassName("auth-hero");
        hero.setPadding(false);
        hero.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        Image logo = new Image("/logo_unet_blanco.png", "Logo UNET");
        logo.addClassName("auth-logo");
        hero.add(logo);

        Span eyebrow = new Span("EDIFICIO C · UNET");
        eyebrow.addClassName("eyebrow");
        H1 title = new H1("La comunidad del C te está esperando.");
        Paragraph text = new Paragraph(
                "Donde encontrás todo lo que el C nos ofrece, y todo lo que en el futuro vamos a construir juntos.");

        hero.add(eyebrow, title, text);

LottieView ocean = new LottieView("/animations/ocean.json");
        ocean.addClassName("auth-lottie");
        hero.add(ocean);

        return hero;
    }

    private VerticalLayout panel() {
        VerticalLayout panel = new VerticalLayout();
        panel.addClassName("auth-panel");
        panel.setPadding(false);
        panel.setSpacing(false);
        panel.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        VerticalLayout card = new VerticalLayout();
        card.addClassName("auth-card");
        card.setPadding(false);

        H1 title = new H1("Iniciar sesión");
        title.addClassName("auth-card-title");

        TextField username = new TextField("Usuario o email");
        PasswordField password = new PasswordField("Contraseña");

        Button login = new Button("Ingresar", event -> doLogin(username.getValue(), password.getValue()));
        login.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button goRegister = new Button("Crear una cuenta", event -> UI.getCurrent().navigate(RegisterView.class));
        goRegister.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        card.add(title, username, password, login, goRegister);
        panel.add(card);
        return panel;
    }

    private void doLogin(String identifier, String secret) {
        try {
            AuthResponse response = api.login(new LoginRequest(identifier, secret));
            session.setAuth(response);
            UI.getCurrent().navigate("home");
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (session.isLoggedIn()) {
            event.forwardTo("home");
        }
    }
}
