package com.redsocial.ui.view;

import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.component.ProfileContent;
import com.redsocial.ui.session.UserSession;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "profile/:username", layout = MainLayout.class)
@PageTitle("Perfil | RED-SOCIAL")
public class UserProfileView extends ProtectedView {

    private final ApiService api;

    public UserProfileView(ApiService api, UserSession session) {
        super(session);
        this.api = api;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        super.beforeEnter(event);
        if (!session.isLoggedIn()) {
            return;
        }
        event.getRouteParameters().get("username").ifPresent(username -> {
            removeAll();
            add(new ProfileContent(username, api, session));
        });
    }
}
