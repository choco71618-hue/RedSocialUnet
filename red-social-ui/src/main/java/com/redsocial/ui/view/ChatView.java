package com.redsocial.ui.view;

import com.redsocial.common.dto.ConversationDto;
import com.redsocial.common.dto.MessageDto;
import com.redsocial.common.dto.PageResponse;
import com.redsocial.common.dto.UserDto;
import com.redsocial.ui.client.ApiException;
import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.session.UserSession;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Objects;

@Route(value = "chat", layout = MainLayout.class)
@PageTitle("Chat | RED-SOCIAL")
public class ChatView extends ProtectedView {

    private final ApiService api;
    private final VerticalLayout conversationList = new VerticalLayout();
    private final VerticalLayout thread = new VerticalLayout();
    private final TextArea input = new TextArea();
    private Long currentConversationId;

    public ChatView(ApiService api, UserSession session) {
        super(session);
        this.api = api;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        conversationList.setPadding(false);
        conversationList.setSpacing(false);
        conversationList.setWidthFull();

        Button newConversation = new Button("Nueva conversación", event -> openNewConversation());
        newConversation.setWidthFull();
        newConversation.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        VerticalLayout leftPane = new VerticalLayout(newConversation, conversationList);
        leftPane.setPadding(true);
        leftPane.setSpacing(true);
        leftPane.setWidth("320px");
        leftPane.addClassName("conversation-list");

        thread.addClassName("message-thread");
        thread.setPadding(true);
        thread.setSpacing(false);
        thread.setWidthFull();

        input.setPlaceholder("Escribí un mensaje...");
        input.setWidthFull();
        input.setMaxLength(2000);

        Button send = new Button("Enviar", event -> sendCurrent());
        send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout composer = new HorizontalLayout(input, send);
        composer.setWidthFull();
        composer.setAlignItems(FlexComponent.Alignment.END);
        composer.setFlexGrow(1, input);
        composer.setPadding(true);

        VerticalLayout threadPane = new VerticalLayout(thread, composer);
        threadPane.setPadding(false);
        threadPane.setSpacing(false);
        threadPane.setSizeFull();
        threadPane.setFlexGrow(1, thread);

        HorizontalLayout shell = new HorizontalLayout(leftPane, threadPane);
        shell.setSizeFull();
        shell.setPadding(false);
        shell.setSpacing(false);
        shell.setFlexGrow(1, threadPane);
        shell.addClassName("chat-shell");

        add(shell);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        super.beforeEnter(event);
        if (!session.isLoggedIn()) {
            return;
        }
        loadConversations();
        String conversation = event.getLocation().getQueryParameters()
                .getSingleParameter("conversation").orElse(null);
        if (conversation != null) {
            try {
                selectConversation(Long.parseLong(conversation));
                return;
            } catch (NumberFormatException ignored) {
                // parámetro inválido, se ignora
            }
        }
        if (currentConversationId == null) {
            thread.removeAll();
            thread.add(new Span("Seleccioná una conversación"));
        } else {
            loadThread();
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        getUI().ifPresent(ui -> {
            ui.setPollInterval(2000);
            ui.addPollListener(event -> {
                if (!session.isLoggedIn()) {
                    return;
                }
                if (currentConversationId != null) {
                    loadThread();
                }
                loadConversations();
            });
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        getUI().ifPresent(ui -> ui.setPollInterval(-1));
        super.onDetach(detachEvent);
    }

    private void loadConversations() {
        conversationList.removeAll();
        try {
            List<ConversationDto> conversations = api.conversations();
            if (conversations.isEmpty()) {
                conversationList.add(new Span("No tenés conversaciones todavía"));
                return;
            }
            for (ConversationDto conversation : conversations) {
                Button button = new Button(label(conversation));
                button.setWidthFull();
                button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                button.getStyle().set("justify-content", "flex-start");
                if (conversation.id().equals(currentConversationId)) {
                    button.getStyle().set("font-weight", "bold");
                }
                button.addClickListener(event -> selectConversation(conversation.id()));
                conversationList.add(button);
            }
        } catch (ApiException ex) {
            Notification.show(ex.getMessage());
        }
    }

    private void selectConversation(Long conversationId) {
        currentConversationId = conversationId;
        try {
            api.markRead(conversationId);
        } catch (ApiException ignored) {
            // si falla el marcado, la conversación igual se muestra
        }
        loadThread();
        loadConversations();
    }

    private void loadThread() {
        thread.removeAll();
        if (currentConversationId == null) {
            thread.add(new Span("Seleccioná una conversación"));
            return;
        }
        try {
            PageResponse<MessageDto> page = api.messages(currentConversationId, 0, 200);
            if (page.content().isEmpty()) {
                thread.add(new Span("Todavía no hay mensajes. ¡Escribí el primero!"));
                return;
            }
            for (MessageDto message : page.content()) {
                Span bubble = new Span(message.content());
                bubble.addClassName("message-bubble");
                boolean mine = session.getUser() != null
                        && Objects.equals(message.senderId(), session.getUser().id());
                if (mine) {
                    bubble.addClassName("mine");
                }
                thread.add(bubble);
            }
        } catch (ApiException ex) {
            Notification.show(ex.getMessage());
        }
    }

    private void sendCurrent() {
        if (currentConversationId == null) {
            Notification.show("Seleccioná una conversación");
            return;
        }
        String text = input.getValue();
        if (text == null || text.isBlank()) {
            return;
        }
        try {
            api.sendMessage(currentConversationId, text);
            input.clear();
            loadThread();
        } catch (ApiException ex) {
            Notification.show(ex.getMessage());
        }
    }

    private void openNewConversation() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nueva conversación");
        dialog.setWidth("480px");

        TextField search = new TextField();
        search.setPlaceholder("Buscar usuario...");
        search.setWidthFull();

        VerticalLayout results = new VerticalLayout();
        results.setPadding(false);

        Button find = new Button("Buscar", event -> {
            results.removeAll();
            try {
                List<UserDto> users = api.search(search.getValue() == null ? "" : search.getValue().trim());
                for (UserDto user : users) {
                    if (session.getUser() != null && Objects.equals(user.id(), session.getUser().id())) {
                        continue;
                    }
                    Button pick = new Button(display(user) + "  @" + user.username());
                    pick.setWidthFull();
                    pick.getStyle().set("justify-content", "flex-start");
                    pick.addClickListener(click -> {
                        try {
                            ConversationDto conversation = api.startConversation(user.id());
                            dialog.close();
                            loadConversations();
                            selectConversation(conversation.id());
                        } catch (ApiException ex) {
                            Notification.show(ex.getMessage());
                        }
                    });
                    results.add(pick);
                }
                if (users.isEmpty()) {
                    results.add(new Span("Sin resultados"));
                }
            } catch (ApiException ex) {
                Notification.show(ex.getMessage());
            }
        });
        find.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout bar = new HorizontalLayout(search, find);
        bar.setWidthFull();
        bar.setAlignItems(FlexComponent.Alignment.END);
        bar.setFlexGrow(1, search);

        VerticalLayout content = new VerticalLayout(bar, results);
        content.setPadding(false);
        dialog.add(content);
        dialog.open();
    }

    private String label(ConversationDto conversation) {
        String name = display(conversation.otherUser());
        return conversation.unreadCount() > 0 ? name + "  (" + conversation.unreadCount() + ")" : name;
    }

    private String display(UserDto user) {
        if (user == null) {
            return "Desconocido";
        }
        return user.displayName() == null || user.displayName().isBlank()
                ? user.username()
                : user.displayName();
    }
}
