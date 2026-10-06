package com.redsocial.ui;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@Theme(themeClass = Lumo.class)
@StyleSheet("styles.css")
public class RedSocialUiApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(RedSocialUiApplication.class, args);
    }
}
