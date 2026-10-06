package com.redsocial.ui.view;

import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.component.ProfileContent;
import com.redsocial.ui.session.UserSession;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "profile", layout = MainLayout.class)
@PageTitle("Perfil | RED-SOCIAL")
public class ProfileView extends ProtectedView {

    public ProfileView(ApiService api, UserSession session) {
        super(session);
        add(new ProfileContent(null, api, session));
    }
}
