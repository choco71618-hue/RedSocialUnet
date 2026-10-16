package com.redsocial.ui.view;

import com.redsocial.ui.session.UserSession;
import com.vaadin.flow.component.dependency.JavaScript;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
@JavaScript("context://authkit/landing.js")
public class LandingView extends Div implements BeforeEnterObserver {

    private static final String LANDING_RESOURCE = "static/authkit/landing.html";

    /**
     * En desarrollo leemos el HTML directamente del código fuente para que los
     * cambios se vean al recargar la página (sin reiniciar la app). Si no existe
     * (p. ej. en el jar empaquetado) se cae al recurso del classpath.
     */
    private static final String[] DEV_PATHS = {
        "src/main/resources/static/authkit/landing.html",
        "red-social-ui/src/main/resources/static/authkit/landing.html"
    };

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
        for (String devPath : DEV_PATHS) {
            Path path = Paths.get(devPath);
            if (Files.isReadable(path)) {
                try {
                    return Files.readString(path, StandardCharsets.UTF_8);
                } catch (IOException ex) {
                    throw new UncheckedIOException("No se pudo leer " + path, ex);
                }
            }
        }
        try (var in = new ClassPathResource(LANDING_RESOURCE).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo cargar " + LANDING_RESOURCE, ex);
        }
    }
}
