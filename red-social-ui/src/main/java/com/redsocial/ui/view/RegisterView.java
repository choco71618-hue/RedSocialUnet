package com.redsocial.ui.view;

import com.redsocial.common.dto.AuthResponse;
import com.redsocial.common.dto.RegisterRequest;
import com.redsocial.common.dto.TutorType;
import com.redsocial.common.dto.UserRole;
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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("register")
@PageTitle("Crear cuenta | RED-SOCIAL")
public class RegisterView extends HorizontalLayout implements BeforeEnterObserver {

    private final ApiService api;
    private final UserSession session;

    public RegisterView(ApiService api, UserSession session) {
        this.api = api;
        this.session = session;

        addClassName("auth-split");

        VerticalLayout hero = hero();
        VerticalLayout panel = panel();
        add(hero, panel);
    }

    private VerticalLayout hero() {
        VerticalLayout hero = new VerticalLayout();
        hero.addClassName("auth-hero");
        hero.addClassName("rs-justify-center");

        Image logo = new Image("/logo_unet_blanco.png", "Logo UNET");
        logo.addClassName("auth-logo");
        hero.add(logo);

        Span eyebrow = new Span("SUMATE AL EDIFICIO C");
        eyebrow.addClassName("eyebrow");
        H1 title = new H1("Tu lugar en el C.");
        Paragraph text = new Paragraph(
                "Creá tu perfil y enterate de todo lo que te espera: eventos, espacios nuevos y los proyectos que vienen.");

        hero.add(eyebrow, title, text);

LottieView ocean = new LottieView("/animations/ocean.json");
        ocean.addClassName("auth-lottie");
        hero.add(ocean);

        return hero;
    }

    private VerticalLayout panel() {
        VerticalLayout panel = new VerticalLayout();
        panel.addClassName("auth-panel");
        panel.addClassName("rs-justify-center");

        VerticalLayout card = new VerticalLayout();
        card.addClassName("auth-card");

        H1 title = new H1("Crear cuenta");
        title.addClassName("auth-card-title");

        TextField displayName = new TextField("Nombre");
        TextField username = new TextField("Usuario");
        TextField email = new TextField("Email");
        PasswordField password = new PasswordField("Contraseña");
        password.setHelperText("Mínimo 8 caracteres");

        Select<UserRole> role = new Select<>();
        role.setLabel("Tipo de cuenta");
        role.setItems(UserRole.values());
        role.setItemLabelGenerator(r -> r == UserRole.STUDENT ? "Estudiante" : "Preparador");
        role.setValue(UserRole.STUDENT);
        role.setRequiredIndicatorVisible(true);

        TextField career = new TextField("Carrera");
        TextField interest = new TextField("Materia de interés");

        Select<TutorType> tutorType = new Select<>();
        tutorType.setLabel("Tipo de preparador");
        tutorType.setItems(TutorType.values());
        tutorType.setItemLabelGenerator(t -> t == TutorType.UNET ? "Preparador UNET" : "Clases privadas");
        tutorType.setValue(TutorType.UNET);

        Runnable sync = () -> {
            boolean student = role.getValue() == UserRole.STUDENT;
            career.setVisible(student);
            interest.setVisible(student);
            tutorType.setVisible(!student);
        };
        role.addValueChangeListener(event -> sync.run());
        sync.run();

        Button register = new Button("Registrarme", event -> doRegister(
                displayName.getValue(), username.getValue(), email.getValue(), password.getValue(),
                role.getValue(), career.getValue(), interest.getValue(), tutorType.getValue()));
        register.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button goLogin = new Button("Ya tengo cuenta", event -> UI.getCurrent().navigate(LoginView.class));
        goLogin.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        card.add(title, displayName, username, email, password, role, career, interest, tutorType, register, goLogin);
        panel.add(card);
        return panel;
    }

    private void doRegister(String displayName, String username, String email, String password,
                            UserRole role, String career, String subjectOfInterest, TutorType tutorType) {
        try {
            AuthResponse response = api.register(new RegisterRequest(
                    username, email, password, displayName, role, career, subjectOfInterest, tutorType));
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
