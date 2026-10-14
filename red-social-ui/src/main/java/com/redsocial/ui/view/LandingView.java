package com.redsocial.ui.view;

import com.redsocial.ui.session.UserSession;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Landing pública que replica authkit.com.
 *
 * <p>Estrategia: se cargan tal cual los dos hojas de estilo reales del sitio
 * (hotlink) más un archivo de overrides local que apunta a las fuentes
 * descargadas (las remotas se bloquean por CORS) y reactiva el scroll. El
 * markup es el HTML SSR original, inyectado como {@code innerHTML}.
 * Los estilos se cargan solo mientras esta vista está activa, así no
 * contaminan el resto de la aplicación.</p>
 */
@Route("")
@PageTitle("AuthKit by WorkOS")
@StyleSheet("https://www.authkit.com/_next/static/chunks/0l0q6~h79kzc..css")
@StyleSheet("https://www.authkit.com/_next/static/chunks/0kn7kgyjdnxlm.css")
@StyleSheet("context://authkit/overrides.css")
public class LandingView extends Div implements BeforeEnterObserver {

    private static final String LANDING_RESOURCE = "static/authkit/landing.html";

    private final UserSession session;

    public LandingView(UserSession session) {
        this.session = session;
        addClassName("ak-host");
        getElement().setProperty("innerHTML", loadLanding());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (session.isLoggedIn()) {
            event.forwardTo("home");
        }
    }

    private static String loadLanding() {
        try (var in = new ClassPathResource(LANDING_RESOURCE).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo cargar " + LANDING_RESOURCE, ex);
        }
    }
}
