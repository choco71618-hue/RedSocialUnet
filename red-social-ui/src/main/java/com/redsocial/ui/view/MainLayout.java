package com.redsocial.ui.view;

import com.redsocial.common.dto.UserDto;
import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.client.RealtimeBridge;
import com.redsocial.ui.component.NotificationsBell;
import com.redsocial.ui.session.UserSession;
import com.redsocial.ui.util.AvatarUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;

import java.util.LinkedHashMap;
import java.util.Map;

public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private final Map<String, Button> navItems = new LinkedHashMap<>();
    private final UserSession session;

    public MainLayout(UserSession session, ApiService api, RealtimeBridge bridge) {
        this.session = session;
        setPrimarySection(Section.DRAWER);
        setDrawerOpened(true);
        addClassName("app-layout");
        addToNavbar(buildNavbar(api));
        addToDrawer(buildSidebar(session, api, bridge));
    }

    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Auto-cierre de toasts (notificaciones del sistema) tras 3.5s
        getElement().executeJs("""
                (() => {
                  if (window.__rsToastAutoClose) return;
                  window.__rsToastAutoClose = true;
                  const TOAST_MS = 3500;
                  const dismiss = (card) => {
                    if (card.parentNode) card.parentNode.removeChild(card);
                  };
                  setInterval(() => {
                    document.querySelectorAll('vaadin-notification-card').forEach((card) => {
                      const host = card.closest && card.closest('vaadin-notification')
                          || (window.__rsToastHosts || (window.__rsToastHosts = new WeakSet())) && null;
                      if (card.__rsTimer) return;
                      card.__rsTimer = setTimeout(() => {
                        card.__rsTimer = null;
                        dismiss(card);
                      }, TOAST_MS);
                    });
                  }, 500);
                  setInterval(() => {
                    document.querySelectorAll('vaadin-notification').forEach((host) => {
                      if (host.__rsTimer && !host.hasAttribute('opened') && host.opened !== true) {
                        // host cerrado, limpiar timers de sus cards
                      }
                    });
                  }, 2000);
                })();
                """);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        String path = event.getLocation().getPath();
        navItems.forEach((route, button) -> {
            boolean active = route.isEmpty()
                    ? (path == null || path.isEmpty())
                    : path.startsWith(route);
            button.getElement().getClassList().set("active", active);
        });
    }

    private HorizontalLayout buildNavbar(ApiService api) {
        com.vaadin.flow.component.textfield.TextField search = new com.vaadin.flow.component.textfield.TextField();
        search.setPlaceholder("Buscar en el C…");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.addClassName("app-search");
        search.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, event ->
                UI.getCurrent().navigate("explore?q=" + (search.getValue() == null ? "" : search.getValue().trim())));

        Button post = new Button("Publicar", VaadinIcon.PAPERPLANE.create(),
                event -> UI.getCurrent().navigate("home"));
        post.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        post.addClassName("app-post-button");

        HorizontalLayout actions = new HorizontalLayout(search, post, userChip());
        actions.addClassName("app-nav-actions");
        actions.setPadding(false);
        actions.setSpacing(false);
        actions.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout navbar = new HorizontalLayout(actions);
        navbar.addClassName("app-navbar");
        navbar.setAlignItems(FlexComponent.Alignment.CENTER);
        return navbar;
    }

    private HorizontalLayout brand() {
        Span mark = new Span("C");
        mark.addClassName("app-brand-mark");
        Span name = new Span("RED-SOCIAL");
        name.addClassName("app-brand-name");
        Span sub = new Span("Edificio C · UNET");
        sub.addClassName("app-brand-sub");
        VerticalLayout text = new VerticalLayout(name, sub);
        text.setPadding(false);
        text.setSpacing(false);
        text.addClassName("app-brand-text");
        HorizontalLayout brand = new HorizontalLayout(mark, text);
        brand.addClassName("app-brand");
        brand.setPadding(false);
        brand.setSpacing(false);
        brand.setAlignItems(FlexComponent.Alignment.CENTER);
        return brand;
    }

    private com.vaadin.flow.component.html.Span userChip() {
        com.vaadin.flow.component.html.Span chip = new com.vaadin.flow.component.html.Span();
        chip.addClassName("app-user-chip");
        UserDto user = session.getUser();
        if (user != null) {
            com.vaadin.flow.component.avatar.Avatar avatar = AvatarUtil.of(user);
            avatar.addClassName("chip-avatar");
            com.vaadin.flow.component.html.Span name = new com.vaadin.flow.component.html.Span(display(user));
            name.addClassName("app-user-chip-name");
            chip.add(avatar, name);
        }
        chip.addClickListener(event -> UI.getCurrent().navigate("profile"));
        return chip;
    }

    private VerticalLayout buildSidebar(UserSession session, ApiService api, RealtimeBridge bridge) {
        VerticalLayout sidebar = new VerticalLayout();
        sidebar.addClassName("app-sidebar");
        sidebar.setPadding(false);
        sidebar.setSpacing(false);

        HorizontalLayout brand = brand();
        brand.addClassName("app-brand-box");

        VerticalLayout nav = new VerticalLayout(
                navItem("home", VaadinIcon.HOME, "Inicio"),
                navItem("explore", VaadinIcon.SEARCH, "Explorar"),
                navItem("projects", VaadinIcon.ROCKET, "Proyectos"),
                navItem("chat", VaadinIcon.COMMENTS, "Chat"),
                navItem("profile", VaadinIcon.USER, "Perfil"));
        nav.addClassName("app-nav");
        nav.setPadding(false);
        nav.setSpacing(false);

        NotificationsBell bell = new NotificationsBell(api, session, bridge);
        bell.addClassName("app-bell");

        Span spacer = new Span();
        spacer.addClassName("app-spacer");

        sidebar.add(brand, nav, bell, spacer, buildUserBox(session));
        return sidebar;
    }

    private Button navItem(String route, VaadinIcon icon, String text) {
        Icon iconComponent = icon.create();
        iconComponent.addClassName("app-nav-icon");
        Button button = new Button(text, iconComponent, event -> UI.getCurrent().navigate(route));
        button.addClassName("app-nav-item");
        navItems.put(route, button);
        return button;
    }

    private VerticalLayout buildUserBox(UserSession session) {
        VerticalLayout box = new VerticalLayout();
        box.addClassName("app-user");
        box.setPadding(false);
        box.setSpacing(false);

        UserDto user = session.getUser();
        if (user != null) {
            Avatar avatar = AvatarUtil.of(user);
            avatar.addClassName("rs-avatar-sm");

            Span name = new Span(display(user));
            name.addClassName("app-user-name");
            Span handle = new Span("@" + user.username());
            handle.addClassName("app-user-handle");
            VerticalLayout info = new VerticalLayout(name, handle);
            info.setPadding(false);
            info.setSpacing(false);

            HorizontalLayout row = new HorizontalLayout(avatar, info);
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            box.add(row);
        }

        Icon out = VaadinIcon.SIGN_OUT.create();
        Button logout = new Button("Salir", out, event -> {
            session.logout();
            UI.getCurrent().navigate(LoginView.class);
        });
        logout.addClassName("app-logout");
        box.add(logout);
        return box;
    }

    private String display(UserDto user) {
        return user.displayName() == null || user.displayName().isBlank()
                ? user.username()
                : user.displayName();
    }
}
