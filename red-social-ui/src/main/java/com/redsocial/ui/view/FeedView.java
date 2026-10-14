package com.redsocial.ui.view;

import com.redsocial.common.dto.MediaDto;
import com.redsocial.common.dto.PageResponse;
import com.redsocial.common.dto.PostDto;
import com.redsocial.common.dto.ProjectDto;
import com.redsocial.common.dto.ProjectKind;
import com.redsocial.common.dto.UserDto;
import com.redsocial.ui.client.ApiException;
import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.component.PostCard;
import com.redsocial.ui.component.Skeleton;
import com.redsocial.ui.session.UserSession;
import com.redsocial.ui.util.AvatarUtil;
import com.redsocial.ui.util.Toast;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Route(value = "home", layout = MainLayout.class)
@PageTitle("Inicio | RED-SOCIAL")
public class FeedView extends ProtectedView {

    private static final DateTimeFormatter MONTH =
            DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("es"));

    private final ApiService api;
    private final UserSession session;
    private final TextArea composer = new TextArea();
    private final VerticalLayout posts = new VerticalLayout();
    private final VerticalLayout rail = new VerticalLayout();
    private final MemoryBuffer buffer = new MemoryBuffer();
    private final Image preview = new Image();
    private final Span count = new Span();
    private String pendingMediaUrl;

    public FeedView(ApiService api, UserSession session) {
        super(session);
        this.api = api;
        this.session = session;

        composer.setPlaceholder("¿Qué está pasando en el C?");
        composer.setMaxLength(2000);
        composer.addValueChangeListener(event -> {
            int len = composer.getValue() == null ? 0 : composer.getValue().length();
            count.setText(len > 0 ? String.valueOf(len) + "/2000" : "");
        });

        Button publish = new Button("Publicar", VaadinIcon.PAPERPLANE.create(), event -> publish());
        publish.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        publish.setEnabled(false);
        composer.addValueChangeListener(event -> publish.setEnabled(composer.getValue() != null
                && !composer.getValue().isBlank()));

        Upload upload = buildUpload();
        preview.addClassName("composer-preview");
        preview.setVisible(false);
        Button removePreview = new Button(VaadinIcon.CLOSE.create(), event -> clearPreview());
        count.addClassName("composer-count");

        HorizontalLayout toolbar = new HorizontalLayout(upload, removePreview, count);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setPadding(false);
        toolbar.setFlexGrow(1, count);

        HorizontalLayout editorRow = new HorizontalLayout(composer, publish);
        editorRow.setAlignItems(FlexComponent.Alignment.END);
        editorRow.setFlexGrow(1, composer);

        VerticalLayout editor = new VerticalLayout(editorRow, preview, toolbar);
        editor.setPadding(false);
        editor.setSpacing(true);

        Avatar avatar = AvatarUtil.of(session.getUser());
        HorizontalLayout composerCard = new HorizontalLayout(avatar, editor);
        composerCard.addClassName("composer");
        composerCard.setAlignItems(FlexComponent.Alignment.START);
        composerCard.setFlexGrow(1, editor);

        posts.setPadding(false);
        posts.setSpacing(false);

        VerticalLayout main = new VerticalLayout(header(), composerCard, posts);
        main.setPadding(false);
        main.setSpacing(false);
        main.addClassName("layout-main");

        rail.addClassName("layout-rail");
        rail.setPadding(false);
        rail.setSpacing(false);

        Div layout = new Div(main, rail);
        layout.addClassName("layout-with-rail");
        add(layout);
    }

    private Upload buildUpload() {
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/png", "image/jpeg", "image/gif", "image/webp");
        upload.setMaxFileSize(8 * 1024 * 1024);
        upload.setDropLabel(new Span("Imagen"));
        upload.addSucceededListener(event -> {
            try {
                byte[] bytes = buffer.getInputStream().readAllBytes();
                String fileName = event.getFileName() == null ? "image.jpg" : event.getFileName();
                MediaDto media = api.uploadMedia(bytes, event.getMIMEType(), fileName);
                pendingMediaUrl = media.url();
                preview.setSrc(media.url());
                preview.setVisible(true);
            } catch (ApiException ex) {
                Toast.show(ex.getMessage());
            } catch (Exception ex) {
                Toast.show("No se pudo subir la imagen");
            }
        });
        upload.addClassName("feed-upload");
        return upload;
    }

    private void clearPreview() {
        pendingMediaUrl = null;
        preview.setSrc("");
        preview.setVisible(false);
    }

    private Div header() {
        Div header = new Div();
        header.addClassName("page-header");
        Span eyebrow = new Span("FEED");
        eyebrow.addClassName("eyebrow");
        Span title = new Span("Inicio");
        title.addClassName("page-title");
        header.add(eyebrow, title);
        return header;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        super.beforeEnter(event);
        if (session.isLoggedIn()) {
            load();
            loadRail();
        }
    }

    private void publish() {
        try {
            api.createPost(composer.getValue(), pendingMediaUrl);
            composer.clear();
            clearPreview();
            load();
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private void load() {
        posts.removeAll();
        showSkeletons();
        try {
            PageResponse<PostDto> page = api.feed(0, 50);
            posts.removeAll();
            if (page.content().isEmpty()) {
                posts.add(emptyState("Tu feed está vacío",
                        "Seguí a otros usuarios desde Explorar o publicá algo para empezar."));
                return;
            }
            page.content().forEach(post -> posts.add(new PostCard(post, api, session, this::load)));
        } catch (ApiException ex) {
            posts.removeAll();
            Toast.show(ex.getMessage());
        }
    }

    private void showSkeletons() {
        for (int i = 0; i < 3; i++) {
            posts.add(Skeleton.card());
        }
    }

    private void loadRail() {
        rail.removeAll();
        try {
            List<ProjectDto> events = api.projects(ProjectKind.EVENT, null);
            railCard(eventsCard(events));
        } catch (ApiException ignored) {
        }
        try {
            List<UserDto> suggestions = api.suggestions();
            rail.add(usersCard(suggestions));
        } catch (ApiException ignored) {
        }
    }

    private Div railCard(com.vaadin.flow.component.Component card) {
        Div wrapper = new Div(card);
        wrapper.addClassName("rail-card");
        rail.add(wrapper);
        return wrapper;
    }

    private VerticalLayout eventsCard(List<ProjectDto> events) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(false);
        card.setSpacing(false);

        Span title = new Span("Próximos eventos del C");
        title.addClassName("rail-title");
        card.add(title);

        events.stream().limit(4).forEach(event -> card.add(eventRow(event)));
        if (events.isEmpty()) {
            Span none = new Span("Todavía no hay eventos.");
            none.addClassName("rail-sub");
            card.add(none);
        }
        return card;
    }

    private Div eventRow(ProjectDto event) {
        Div row = new Div();
        row.addClassName("rail-row");
        Span day = new Span(event.eventDate() == null ? "?" : String.valueOf(event.eventDate().getDayOfMonth()));
        Span month = new Span(event.eventDate() == null ? "???" : MONTH.format(event.eventDate()));
        Span link = new Span(event.title());
        link.addClassName("rail-link");
        Span when = new Span(day.getText() + " " + month.getText());
        when.addClassName("rail-sub");
        row.add(link);
        row.add(when);
        row.addClickListener(e -> UI.getCurrent().navigate("projects"));
        return row;
    }

    private VerticalLayout usersCard(List<UserDto> users) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(false);
        card.setSpacing(false);

        Span title = new Span("A quién seguir");
        title.addClassName("rail-title");
        card.add(title);

        users.stream().limit(3).forEach(user -> card.add(userRow(user)));
        if (users.isEmpty()) {
            Span none = new Span("Por ahora no hay sugerencias.");
            none.addClassName("rail-sub");
            card.add(none);
        }
        return card;
    }

    private Div userRow(UserDto user) {
        Div row = new Div();
        row.addClassName("rail-row");
        Avatar avatar = AvatarUtil.of(user);
        avatar.addClassName("rail-avatar");
        Span name = new Span(display(user));
        name.addClassName("rail-link");
        Span handle = new Span("@" + user.username());
        handle.addClassName("rail-sub");
        VerticalLayout info = new VerticalLayout(name, handle);
        info.setPadding(false);
        info.setSpacing(false);

        Button follow = new Button("Seguir", event -> {
            try {
                api.follow(user.username());
                loadRail();
            } catch (ApiException ex) {
                Toast.show(ex.getMessage());
            }
        });
        follow.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        follow.addClassName("rail-follow");

        row.add(avatar, info, follow);
        return row;
    }

    private Div emptyState(String title, String subtitle) {
        Div div = new Div();
        div.addClassName("empty-state");
        Span icon = new Span(VaadinIcon.INBOX.create());
        icon.addClassName("empty-icon");
        Span titleSpan = new Span(title);
        titleSpan.addClassName("empty-state-title");
        Span subSpan = new Span(subtitle);
        div.add(icon, titleSpan, subSpan);
        return div;
    }

    private String display(UserDto user) {
        return user == null ? "" : (user.displayName() == null || user.displayName().isBlank()
                ? user.username()
                : user.displayName());
    }
}