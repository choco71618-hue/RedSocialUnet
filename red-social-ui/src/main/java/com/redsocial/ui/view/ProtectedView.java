package com.redsocial.ui.view;

import com.redsocial.ui.session.UserSession;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

public abstract class ProtectedView extends VerticalLayout implements BeforeEnterObserver {

    protected final UserSession session;

    protected ProtectedView(UserSession session) {
        this.session = session;
        addClassName("page-container");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!session.isLoggedIn()) {
            event.forwardTo(LoginView.class);
        }
    }
}
