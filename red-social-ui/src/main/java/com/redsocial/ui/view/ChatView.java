package com.redsocial.ui.view;

import com.redsocial.common.dto.ConversationDto;
import com.redsocial.common.dto.MessageDto;
import com.redsocial.common.dto.PageResponse;
import com.redsocial.common.dto.UserDto;
import com.redsocial.ui.client.ApiException;
import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.client.RealtimeBridge;
import com.redsocial.ui.session.UserSession;
import com.redsocial.ui.util.AvatarUtil;
import com.redsocial.ui.util.Toast;
import com.redsocial.ui.util.TimeFormat;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
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
    private final RealtimeBridge bridge;
    private final VerticalLayout conversationList = new VerticalLayout();
    private final VerticalLayout thread = new VerticalLayout();
    private final TextArea input = new TextArea();
    private final Span typing = new Span();
    private HorizontalLayout shell;
    private Long currentConversationId;
    private final RealtimeBridge.Consumer<MessageDto> messageListener = dto -> {
        if (dto != null && Objects.equals(dto.conversationId(), currentConversationId)
                && session.getUser() != null && !Objects.equals(dto.senderId(), session.getUser().id())) {
            loadThread();
            loadConversations();
        }
    };
    private final RealtimeBridge.Consumer<RealtimeBridge.TypingInfo> typingListener = info -> {
        if (info != null && Objects.equals(info.conversationId(), currentConversationId)
                && session.getUser() != null && !Objects.equals(info.username(), session.getUser().username())) {
            updateTyping(info.typing());
        }
    };

    public ChatView(ApiService api, UserSession session, RealtimeBridge bridge) {
        super(session);
        this.api = api;
        this.bridge = bridge;

        Button newConversation = new Button("Nueva conversación", VaadinIcon.PLUS.create(),
                event -> openNewConversation());
        newConversation.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        VerticalLayout leftPane = new VerticalLayout(newConversation, conversationList);
        leftPane.addClassName("conversation-list");
        leftPane.addClassName("rs-gap");

        thread.addClassName("message-thread");

        typing.addClassName("typing-indicator");
        typing.setVisible(false);

        input.setPlaceholder("Escribí un mensaje...");
        input.setMaxLength(2000);
        input.addValueChangeListener(event -> {
            if (currentConversationId != null) {
                bridge.sendTyping(currentConversationId, !input.getValue().isEmpty());
            }
        });

        Button send = new Button("Enviar", VaadinIcon.PAPERPLANE.create(), event -> sendCurrent());
        send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout composer = new HorizontalLayout(input, send);
        composer.addClassName("chat-composer");
        composer.addClassName("rs-items-end");
        input.addClassName("rs-grow");

        Button back = new Button("Volver", VaadinIcon.ARROW_LEFT.create(), event -> showList());
        back.addClassName("chat-back");
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        VerticalLayout threadPane = new VerticalLayout(back, thread, typing, composer);
        threadPane.addClassName("thread-pane");
        threadPane.addClassName("rs-grow");

        shell = new HorizontalLayout(leftPane, threadPane);
        shell.addClassName("chat-shell");
        threadPane.addClassName("rs-grow");

        add(header(), shell);
    }

    private void showList() {
        if (shell != null) {
            shell.getElement().getClassList().set("is-thread", false);
        }
    }

    private Div header() {
        Div header = new Div();
        header.addClassName("page-header");
        Span eyebrow = new Span("Mensajería");
        eyebrow.addClassName("eyebrow");
        Span title = new Span("Chat del Edificio C");
        title.addClassName("page-title");
        header.add(eyebrow, title);
        return header;
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
            showList();
            thread.removeAll();
            thread.add(emptyState("Elegí una conversación", "Seleccioná un chat de la izquierda o creá uno nuevo."));
        } else {
            loadThread();
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        getUI().ifPresent(ui -> {
            if (session.getToken() != null) {
                bridge.connect(session.getToken(), ui);
            }
        });
    }

    private void updateTyping(boolean active) {
        if (active) {
            typing.setText("Escribiendo…");
            typing.setVisible(true);
        } else {
            typing.setVisible(false);
            typing.setText("");
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        unsubscribeCurrent();
        super.onDetach(detachEvent);
    }

    private void subscribeCurrent() {
        bridge.subscribeConversation(currentConversationId, messageListener);
        bridge.addTypingHandler(String.valueOf(currentConversationId), typingListener);
    }

    private void unsubscribeCurrent() {
        if (currentConversationId != null) {
            bridge.unsubscribeConversation(currentConversationId);
            bridge.removeTypingHandler(String.valueOf(currentConversationId));
        }
    }

    private void loadConversations() {
        conversationList.removeAll();
        for (int i = 0; i < 3; i++) {
            conversationList.add(com.redsocial.ui.component.Skeleton.card());
        }
        try {
            List<ConversationDto> conversations = api.conversations();
            if (conversations.isEmpty()) {
                conversationList.add(emptyState("Sin conversaciones", "Creá una nueva para empezar."));
                return;
            }
            for (ConversationDto conversation : conversations) {
                conversationList.add(conversationItem(conversation));
            }
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private Div conversationItem(ConversationDto conversation) {
        Div item = new Div();
        item.addClassName("conv-item");
        if (conversation.id().equals(currentConversationId)) {
            item.addClassName("active");
        }

        Span name = new Span(display(conversation.otherUser()));
        name.addClassName("post-author");
        Span preview = new Span(conversation.lastMessage() == null ? "Sin mensajes" : conversation.lastMessage());
        preview.addClassName("conv-preview");

        VerticalLayout info = new VerticalLayout(name, preview);
        info.addClassName("conv-info");

        item.add(AvatarUtil.of(conversation.otherUser()), info);
        if (conversation.unreadCount() > 0) {
            Span badge = new Span(String.valueOf(conversation.unreadCount()));
            badge.addClassName("conv-badge");
            item.add(badge);
        }
        item.addClickListener(event -> selectConversation(conversation.id()));
        return item;
    }

    private void selectConversation(Long conversationId) {
        unsubscribeCurrent();
        currentConversationId = conversationId;
        subscribeCurrent();
        if (shell != null) {
            shell.getElement().getClassList().set("is-thread", true);
        }
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
            thread.add(emptyState("Elegí una conversación", "Seleccioná un chat de la izquierda."));
            return;
        }
        try {
            PageResponse<MessageDto> page = api.messages(currentConversationId, 0, 200);
            if (page.content().isEmpty()) {
                thread.add(emptyState("Sin mensajes", "Escribí el primero para romper el hielo."));
                return;
            }
            for (MessageDto message : page.content()) {
                boolean mine = session.getUser() != null
                        && Objects.equals(message.senderId(), session.getUser().id());
                Span bubble = new Span(message.content());
                bubble.addClassName("message-bubble");
                Span time = new Span(TimeFormat.relative(message.createdAt()));
                time.addClassName("message-time");
                if (mine) {
                    bubble.addClassName("mine");
                    time.addClassName("mine");
                }
                thread.add(bubble, time);
            }
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private void sendCurrent() {
        if (currentConversationId == null) {
            Toast.show("Seleccioná una conversación");
            return;
        }
        String text = input.getValue();
        if (text == null || text.isBlank()) {
            return;
        }
        try {
            api.sendMessage(currentConversationId, text);
            input.clear();
            bridge.sendTyping(currentConversationId, false);
            loadThread();
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private void openNewConversation() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nueva conversación");

        TextField search = new TextField();
        search.setPlaceholder("Buscar usuario...");

        VerticalLayout results = new VerticalLayout();
        results.addClassName("rs-gap-sm");

        Button find = new Button("Buscar", VaadinIcon.SEARCH.create(), event -> {
            results.removeAll();
            try {
                List<UserDto> users = api.search(search.getValue() == null ? "" : search.getValue().trim());
                for (UserDto user : users) {
                    if (session.getUser() != null && Objects.equals(user.id(), session.getUser().id())) {
                        continue;
                    }
                    results.add(userResult(user, dialog));
                }
                if (users.isEmpty()) {
                    results.add(new Span("Sin resultados"));
                }
            } catch (ApiException ex) {
                Toast.show(ex.getMessage());
            }
        });
        find.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout bar = new HorizontalLayout(search, find);
        bar.addClassName("rs-items-end");
        search.addClassName("rs-grow");

        VerticalLayout content = new VerticalLayout(bar, results);
        content.addClassName("rs-gap");
        dialog.add(content);
        dialog.open();
    }

    private Div userResult(UserDto user, Dialog dialog) {
        Span name = new Span(display(user));
        name.addClassName("post-author");
        Span handle = new Span("@" + user.username());
        handle.addClassName("post-handle");
        VerticalLayout info = new VerticalLayout(name, handle);

        Div card = new Div();
        card.addClassName("user-card");
        card.addClassName("chat-user-row");
        card.add(AvatarUtil.of(user), info);
        card.addClickListener(event -> {
            try {
                ConversationDto conversation = api.startConversation(user.id());
                dialog.close();
                loadConversations();
                selectConversation(conversation.id());
            } catch (ApiException ex) {
                Toast.show(ex.getMessage());
            }
        });
        return card;
    }

    private Div emptyState(String title, String subtitle) {
        Div div = new Div();
        div.addClassName("empty-state");
        Span icon = new Span(VaadinIcon.COMMENTS.create());
        icon.addClassName("empty-icon");
        Span titleSpan = new Span(title);
        titleSpan.addClassName("empty-state-title");
        div.add(icon, titleSpan, new Span(subtitle));
        return div;
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
