package com.redsocial.ui.component;

import com.redsocial.common.dto.NotificationDto;
import com.redsocial.common.dto.NotificationType;
import com.redsocial.ui.client.ApiException;
import com.redsocial.ui.util.Toast;
import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.client.RealtimeBridge;
import com.redsocial.ui.session.UserSession;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.QueryParameters;

import java.util.List;
import java.util.Map;

public class NotificationsBell extends Div {

    private final ApiService api;
    private final UserSession session;
    private final RealtimeBridge bridge;
    private final Span badge = new Span();
    private RealtimeBridge.Consumer<NotificationDto> listener;

    public NotificationsBell(ApiService api, UserSession session, RealtimeBridge bridge) {
        this.api = api;
        this.session = session;
        this.bridge = bridge;
        addClassName("notifications-bell");

        Button bell = new Button(VaadinIcon.BELL.create(), event -> open());
        bell.addClassNames("notifications-bell-button");

        badge.addClassName("notifications-badge");
        badge.setVisible(false);

        getElement().appendChild(bell.getElement(), badge.getElement());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        getUI().ifPresent(ui -> {
            if (session.getToken() != null) {
                bridge.connect(session.getToken(), ui);
            }
        });
        refresh();
        listener = dto -> {
            refresh();
            Toast.show(dto.text());
        };
        bridge.addNotificationHandler(listener);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (listener != null) {
            bridge.removeNotificationHandler(listener);
        }
        super.onDetach(detachEvent);
    }

    private void refresh() {
        try {
            long unread = api.unreadNotifications();
            badge.setText(unread > 0 ? String.valueOf(unread) : "");
            badge.setVisible(unread > 0);
        } catch (ApiException ignored) {
        }
    }

    private void open() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Notificaciones");

        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(false);
        dialog.add(list);

        Button markAll = new Button("Marcar todas leídas", event -> {
            try {
                api.markAllNotificationsRead();
                refresh();
                renderList(list, dialog);
            } catch (ApiException ex) {
                Toast.show(ex.getMessage());
            }
        });
        markAll.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button close = new Button("Cerrar", event -> dialog.close());
        dialog.getFooter().add(markAll, close);
        dialog.open();

        renderList(list, dialog);
    }

    private void renderList(VerticalLayout list, Dialog dialog) {
        list.removeAll();
        try {
            List<NotificationDto> notifications = api.notifications();
            if (notifications.isEmpty()) {
                list.add(empty("Sin notificaciones", "Cuando alguien interactúe, vas a verlo acá."));
                return;
            }
            for (NotificationDto dto : notifications) {
                list.add(row(dto, dialog));
            }
        } catch (ApiException ex) {
            list.add(empty("Error", ex.getMessage()));
        }
    }

    private Div empty(String title, String subtitle) {
        Div div = new Div();
        div.addClassName("empty-state");
        Span t = new Span(title);
        t.addClassName("empty-title");
        div.add(t, new Span(subtitle));
        return div;
    }

    private Div row(NotificationDto dto, Dialog dialog) {
        Div row = new Div();
        row.addClassName("notification-row");
        if (!dto.read()) {
            row.addClassName("unread");
        }

        Span icon = new Span(iconName(dto.type()));
        icon.addClassName("notification-icon");

        VerticalLayout text = new VerticalLayout();
        text.setPadding(false);
        text.setSpacing(false);
        Span body = new Span(dto.text());
        body.addClassName("notification-text");
        text.add(body);

        Div content = new Div(icon, text);
        content.addClassName("notification-content");
        row.add(content);

        row.addClickListener(event -> {
            try {
                api.markNotificationRead(dto.id());
                refresh();
                dialog.close();
                navigateTo(dto);
            } catch (ApiException ignored) {
            }
        });
        return row;
    }

    private void navigateTo(NotificationDto dto) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return;
        }
        switch (dto.type()) {
            case FOLLOW -> ui.navigate("profile/" + dto.actorUsername());
            case MESSAGE -> ui.navigate("chat",
                    QueryParameters.simple(Map.of("conversation", String.valueOf(dto.targetId()))));
            case DONATION -> ui.navigate("projects");
            case LIKE, COMMENT -> ui.navigate("home");
        }
    }

    private String iconName(NotificationType type) {
        return switch (type) {
            case FOLLOW -> "→";
            case LIKE -> "♥";
            case COMMENT -> "💬";
            case MESSAGE -> "✉";
            case DONATION -> "$";
        };
    }
}