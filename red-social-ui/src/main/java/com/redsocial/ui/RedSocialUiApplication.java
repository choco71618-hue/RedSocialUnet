package com.redsocial.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@Theme("redsocial")
@Push(transport = Transport.WEBSOCKET_XHR)
public class RedSocialUiApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(RedSocialUiApplication.class, args);
    }
}
