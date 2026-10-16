package com.redsocial.ui.view;

import com.redsocial.common.dto.ConnectionDto;
import com.redsocial.common.dto.ConnectionStatus;
import com.redsocial.common.dto.ConversationDto;
import com.redsocial.common.dto.ReviewDto;
import com.redsocial.common.dto.TutorDto;
import com.redsocial.common.dto.TutorSubjectDto;
import com.redsocial.common.dto.TutorType;
import com.redsocial.common.dto.UserDto;
import com.redsocial.ui.client.ApiException;
import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.component.Skeleton;
import com.redsocial.ui.session.UserSession;
import com.redsocial.ui.util.AvatarUtil;
import com.redsocial.ui.util.Toast;
import com.redsocial.ui.util.TimeFormat;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Route(value = "preparadurias", layout = MainLayout.class)
@PageTitle("Preparadurías | RED-SOCIAL")
public class PreparaduriasView extends ProtectedView {

    private static final String ROLE_TUTOR = "TUTOR";
    private static final String ROLE_STUDENT = "STUDENT";

    private final ApiService api;

    private final TextField search = new TextField();
    private final TextField subjectFilter = new TextField();
    private final Select<TutorType> typeFilter = new Select<>();
    private final VerticalLayout tutorResults = new VerticalLayout();
    private final VerticalLayout myConnections = new VerticalLayout();

    private final VerticalLayout mySubjectsBox = new VerticalLayout();
    private final VerticalLayout pendingBox = new VerticalLayout();
    private final VerticalLayout studentsBox = new VerticalLayout();
    private final VerticalLayout reviewsBox = new VerticalLayout();

    public PreparaduriasView(ApiService api, UserSession session) {
        super(session);
        this.api = api;
        add(header());
        if (isTutor()) {
            add(tutorPanel());
        } else {
            add(browsePanel());
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (isTutor()) {
            loadTutorPanel();
        } else {
            loadTutors();
            loadConnections();
        }
    }

    private boolean isTutor() {
        UserDto me = session.getUser();
        return me != null && ROLE_TUTOR.equals(me.role());
    }

    private boolean isStudent() {
        UserDto me = session.getUser();
        return me != null && ROLE_STUDENT.equals(me.role());
    }

    // ============================ Header ============================

    private Div header() {
        Div header = new Div();
        header.addClassName("page-header");
        Span eyebrow = new Span("Comunidad · Preparadurías");
        eyebrow.addClassName("eyebrow");
        Span title = new Span("Encontrá tu preparador");
        title.addClassName("page-title");
        header.add(eyebrow, title);
        return header;
    }

    // ============================ Student / browse ============================

    private VerticalLayout browsePanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.addClassName("prepa-panel-root");

        search.setPlaceholder("Buscar preparador por nombre…");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.addClassName("rs-grow");
        search.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, event -> loadTutors());

        subjectFilter.setPlaceholder("Materia (ej: Matemática)");
        subjectFilter.setClearButtonVisible(true);

        typeFilter.setLabel(null);
        typeFilter.setPlaceholder("Tipo de preparador");
        typeFilter.setItems(TutorType.values());
        typeFilter.setItemLabelGenerator(t -> t == TutorType.UNET ? "Preparador UNET" : "Clases privadas");
        typeFilter.setEmptySelectionAllowed(true);
        typeFilter.setEmptySelectionCaption("Todos los tipos");

        Button searchButton = new Button("Buscar", VaadinIcon.SEARCH.create(), event -> loadTutors());
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button clear = new Button("Limpiar", event -> {
            search.clear();
            subjectFilter.clear();
            typeFilter.clear();
            loadTutors();
        });
        clear.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout filters = new HorizontalLayout(search, subjectFilter, typeFilter, searchButton, clear);
        filters.addClassName("prepa-filters");
        filters.addClassName("rs-items-end");

        tutorResults.addClassName("rs-gap");
        myConnections.addClassName("rs-gap");

        VerticalLayout browse = card("Preparadores disponibles", filters, tutorResults);
        panel.add(browse, card("Mis conexiones", myConnections));
        return panel;
    }

    private void loadTutors() {
        tutorResults.removeAll();
        for (int i = 0; i < 3; i++) {
            tutorResults.add(Skeleton.card());
        }
        try {
            List<TutorDto> tutors = api.tutors(search.getValue(), subjectFilter.getValue(), typeFilter.getValue());
            tutorResults.removeAll();
            if (tutors.isEmpty()) {
                tutorResults.add(emptyState(VaadinIcon.ACADEMY_CAP, "Sin preparadores",
                        "Probá con otra materia o quitá los filtros."));
                return;
            }
            tutors.forEach(tutor -> tutorResults.add(tutorCard(tutor)));
        } catch (ApiException ex) {
            tutorResults.removeAll();
            tutorResults.add(emptyState(VaadinIcon.WARNING, "Error", ex.getMessage()));
        }
    }

    private Div tutorCard(TutorDto tutor) {
        Div card = new Div();
        card.addClassName("tutor-card");

        Avatar avatar = AvatarUtil.create(tutor.displayName(), tutor.username(), tutor.avatarUrl());
        avatar.addClassName("rs-avatar-md");

        Span name = new Span(display(tutor.displayName(), tutor.username()));
        name.addClassName("tutor-name");
        name.addClickListener(event -> UI.getCurrent().navigate("profile/" + tutor.username()));
        Span handle = new Span("@" + tutor.username());
        handle.addClassName("post-handle");

        HorizontalLayout head = new HorizontalLayout(avatar, new VerticalLayout(name, handle));
        head.addClassName("tutor-head");
        head.addClassName("rs-items-center");
        VerticalLayout headInfo = (VerticalLayout) head.getComponentAt(1);
        headInfo.addClassName("tutor-head-info");

        Div cardHead = new Div(head);
        cardHead.addClassName("tutor-card-head");
        if (tutor.tutorType() != null) {
            Span badge = new Span(tutor.tutorType() == TutorType.UNET ? "UNET" : "Clases privadas");
            badge.addClassName("tutor-type-badge");
            badge.addClassName(tutor.tutorType() == TutorType.UNET ? "type-unet" : "type-private");
            cardHead.add(badge);
        }

        card.add(cardHead);

        if (tutor.subjects() != null && !tutor.subjects().isEmpty()) {
            Div subjects = new Div();
            subjects.addClassName("tutor-subjects");
            for (TutorSubjectDto subject : tutor.subjects()) {
                Span pill = new Span(subject.subjectName() + " · " + money(subject.pricePerHour()) + "/h");
                pill.addClassName("subject-pill");
                subjects.add(pill);
            }
            card.add(subjects);
        } else {
            Span none = new Span("Todavía no cargó materias.");
            none.addClassName("tutor-empty-subjects");
            card.add(none);
        }

        HorizontalLayout footer = new HorizontalLayout();
        footer.addClassName("tutor-footer");
        footer.addClassName("rs-items-center");
        footer.addClassName("rs-justify-between");

        Span rating = new Span(ratingText(tutor.averageRating(), tutor.reviewCount()));
        rating.addClassName("rating-text");
        footer.add(rating);

        footer.add(actionFor(tutor));
        card.add(footer);

        if (tutor.bio() != null && !tutor.bio().isBlank()) {
            Span bio = new Span(tutor.bio());
            bio.addClassName("tutor-bio");
            card.add(bio);
        }
        return card;
    }

    private com.vaadin.flow.component.Component actionFor(TutorDto tutor) {
        ConnectionStatus status = tutor.connectionStatus();
        if (status == ConnectionStatus.ACCEPTED) {
            Button chat = new Button("Chat", VaadinIcon.COMMENTS.create(), event -> openChat(tutor.id()));
            chat.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            return chat;
        }
        if (status == ConnectionStatus.PENDING) {
            Span span = new Span("Solicitud pendiente");
            span.addClassName("status-pill");
            span.addClassName("status-pending");
            return span;
        }
        if (status == ConnectionStatus.REJECTED) {
            Span span = new Span("Solicitud rechazada");
            span.addClassName("status-pill");
            span.addClassName("status-rejected");
            return span;
        }
        if (!isStudent()) {
            Span span = new Span("Solo estudiantes pueden solicitar");
            span.addClassName("status-pill");
            span.addClassName("status-muted");
            return span;
        }
        Button request = new Button("Solicitar conexión", VaadinIcon.CONNECT.create(),
                event -> requestConnection(tutor.id()));
        request.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return request;
    }

    private void requestConnection(Long tutorId) {
        try {
            api.requestConnection(tutorId);
            Toast.show("Solicitud enviada");
            loadTutors();
            loadConnections();
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private void loadConnections() {
        myConnections.removeAll();
        for (int i = 0; i < 2; i++) {
            myConnections.add(Skeleton.card());
        }
        try {
            UserDto me = session.getUser();
            List<ConnectionDto> all = api.connections();
            List<ConnectionDto> mine = all.stream()
                    .filter(c -> me != null && Objects.equals(c.studentId(), me.id()))
                    .toList();
            myConnections.removeAll();
            if (mine.isEmpty()) {
                myConnections.add(emptyState(VaadinIcon.CONNECT, "Sin conexiones",
                        "Solicitá una preparaduría para empezar."));
                return;
            }
            mine.forEach(c -> myConnections.add(connectionRow(c, false)));
        } catch (ApiException ex) {
            myConnections.removeAll();
            myConnections.add(emptyState(VaadinIcon.WARNING, "Error", ex.getMessage()));
        }
    }

    // ============================ Tutor panel ============================

    private VerticalLayout tutorPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.addClassName("prepa-panel-root");

        panel.add(card("Mi panel de preparador"));
        panel.add(card("Mis materias", subjectForm(), mySubjectsBox));
        panel.add(card("Solicitudes pendientes", pendingBox));
        panel.add(card("Mis estudiantes", studentsBox));
        panel.add(card("Reseñas recibidas", reviewsBox));
        return panel;
    }

    private HorizontalLayout subjectForm() {
        TextField name = new TextField("Materia");
        name.setPlaceholder("Ej: Matemática I");

        NumberField price = new NumberField("Precio por hora");
        price.setPlaceholder("0.00");
        price.setMin(0.01);
        price.setPrefixComponent(VaadinIcon.MONEY.create());

        Button add = new Button("Agregar", VaadinIcon.PLUS.create(), event -> {
            Double value = price.getValue();
            if (name.getValue() == null || name.getValue().isBlank() || value == null || value <= 0) {
                Toast.show("Completá materia y precio válido");
                return;
            }
            try {
                api.addTutorSubject(name.getValue().trim(), BigDecimal.valueOf(value));
                Toast.show("Materia agregada");
                name.clear();
                price.clear();
                loadSubjects();
            } catch (ApiException ex) {
                Toast.show(ex.getMessage());
            }
        });
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout row = new HorizontalLayout(name, price, add);
        row.addClassName("rs-items-end");
        name.addClassName("rs-grow");
        return row;
    }

    private void loadTutorPanel() {
        loadSubjects();
        loadTutorConnections();
        loadReviews();
    }

    private void loadSubjects() {
        mySubjectsBox.removeAll();
        try {
            List<TutorSubjectDto> subjects = api.myTutorSubjects();
            if (subjects.isEmpty()) {
                mySubjectsBox.add(emptyState(VaadinIcon.ACADEMY_CAP, "Sin materias",
                        "Agregá las materias que enseñás para que los estudiantes te encuentren."));
                return;
            }
            subjects.forEach(subject -> mySubjectsBox.add(subjectRow(subject)));
        } catch (ApiException ex) {
            mySubjectsBox.removeAll();
            mySubjectsBox.add(emptyState(VaadinIcon.WARNING, "Error", ex.getMessage()));
        }
    }

    private Div subjectRow(TutorSubjectDto subject) {
        Div row = new Div();
        row.addClassName("subject-row");
        Span name = new Span(subject.subjectName());
        name.addClassName("subject-row-name");
        Span price = new Span(money(subject.pricePerHour()) + " / hora");
        price.addClassName("subject-row-price");

        Button remove = new Button(VaadinIcon.TRASH.create(), event -> {
            try {
                api.removeTutorSubject(subject.id());
                Toast.show("Materia quitada");
                loadSubjects();
            } catch (ApiException ex) {
                Toast.show(ex.getMessage());
            }
        });
        remove.addThemeVariants(ButtonVariant.LUMO_ERROR);
        remove.addClassName("action-btn");

        row.add(name, price, remove);
        return row;
    }

    private void loadTutorConnections() {
        pendingBox.removeAll();
        studentsBox.removeAll();
        try {
            UserDto me = session.getUser();
            List<ConnectionDto> mine = api.connections().stream()
                    .filter(c -> me != null && Objects.equals(c.tutorId(), me.id()))
                    .toList();

            List<ConnectionDto> pending = mine.stream()
                    .filter(c -> c.status() == ConnectionStatus.PENDING).toList();
            List<ConnectionDto> accepted = mine.stream()
                    .filter(c -> c.status() == ConnectionStatus.ACCEPTED).toList();

            if (pending.isEmpty()) {
                pendingBox.add(emptyState(VaadinIcon.INBOX, "Sin solicitudes",
                        "Cuando un estudiante solicite una conexión, aparecerá acá."));
            } else {
                pending.forEach(c -> pendingBox.add(connectionRow(c, true)));
            }

            if (accepted.isEmpty()) {
                studentsBox.add(emptyState(VaadinIcon.USERS, "Sin estudiantes",
                        "Aceptá solicitudes para empezar a dar preparadurías."));
            } else {
                accepted.forEach(c -> studentsBox.add(connectionRow(c, true)));
            }
        } catch (ApiException ex) {
            pendingBox.add(emptyState(VaadinIcon.WARNING, "Error", ex.getMessage()));
        }
    }

    private void loadReviews() {
        reviewsBox.removeAll();
        UserDto me = session.getUser();
        if (me == null) {
            return;
        }
        try {
            List<ReviewDto> reviews = api.tutorReviews(me.username());
            if (reviews.isEmpty()) {
                reviewsBox.add(emptyState(VaadinIcon.STAR, "Sin reseñas",
                        "Todavía no recibiste reseñas de estudiantes."));
                return;
            }
            reviews.forEach(review -> reviewsBox.add(reviewRow(review)));
        } catch (ApiException ex) {
            reviewsBox.removeAll();
            reviewsBox.add(emptyState(VaadinIcon.WARNING, "Error", ex.getMessage()));
        }
    }

    // ============================ Connection rows ============================

    private Div connectionRow(ConnectionDto connection, boolean asTutor) {
        Div row = new Div();
        row.addClassName("connection-row");

        String otherName = asTutor
                ? display(connection.studentDisplayName(), connection.studentUsername())
                : display(connection.tutorDisplayName(), connection.tutorUsername());
        String otherUser = asTutor ? connection.studentUsername() : connection.tutorUsername();

        Span name = new Span(otherName);
        name.addClassName("connection-name");
        Span handle = new Span("@" + otherUser + " · " + TimeFormat.relative(connection.createdAt()));
        handle.addClassName("post-handle");
        VerticalLayout info = new VerticalLayout(name, handle);
        info.addClassName("connection-info");

        Span status = new Span(statusLabel(connection.status()));
        status.addClassName("status-pill");
        status.addClassName("status-" + connection.status().name().toLowerCase());

        HorizontalLayout actions = new HorizontalLayout();
        actions.addClassName("rs-items-center");

        if (connection.status() == ConnectionStatus.PENDING && asTutor) {
            Button accept = new Button("Aceptar", event -> respond(connection.id(), true));
            accept.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            Button reject = new Button("Rechazar", event -> respond(connection.id(), false));
            reject.addThemeVariants(ButtonVariant.LUMO_ERROR);
            actions.add(accept, reject);
        }

        if (connection.status() == ConnectionStatus.ACCEPTED) {
            Button chat = new Button("Chat", VaadinIcon.COMMENTS.create(),
                    event -> openChat(asTutor ? connection.studentId() : connection.tutorId()));
            chat.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            actions.add(chat);

            if (!asTutor && isStudent()) {
                Button review = new Button("Reseñar", VaadinIcon.STAR.create(),
                        event -> openReviewDialog(connection));
                review.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                actions.add(review);
            }

            if (connection.reviewCode() != null && !connection.reviewCode().isBlank()) {
                Span code = new Span("Código: " + connection.reviewCode());
                code.addClassName("review-code");
                actions.add(code);
            }
        }

        row.add(info, status, actions);
        row.addClassName("rs-items-center");
        info.addClassName("rs-grow");
        return row;
    }

    private void respond(Long connectionId, boolean accept) {
        try {
            if (accept) {
                api.acceptConnection(connectionId);
                Toast.show("Conexión aceptada");
            } else {
                api.rejectConnection(connectionId);
                Toast.show("Solicitud rechazada");
            }
            loadTutorConnections();
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private void openChat(Long otherUserId) {
        try {
            ConversationDto conversation = api.startConversation(otherUserId);
            UI.getCurrent().navigate("chat?conversation=" + conversation.id());
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private void openReviewDialog(ConnectionDto connection) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Reseñar a " + display(connection.tutorDisplayName(), connection.tutorUsername()));

        Select<Integer> rating = new Select<>();
        rating.setLabel("Calificación");
        rating.setItems(1, 2, 3, 4, 5);
        rating.setItemLabelGenerator(r -> r + " ★");
        rating.setValue(5);

        TextArea comment = new TextArea("Comentario");
        comment.setMaxLength(1000);
        comment.setPlaceholder("Contá cómo fue tu experiencia…");

        TextField code = new TextField("Código de reseña");
        code.setValue(connection.reviewCode() == null ? "" : connection.reviewCode());

        Button submit = new Button("Enviar reseña", event -> {
            try {
                api.reviewConnection(connection.id(), rating.getValue(), comment.getValue(), code.getValue());
                dialog.close();
                Toast.show("¡Gracias por tu reseña!");
                loadConnections();
            } catch (ApiException ex) {
                Toast.show(ex.getMessage());
            }
        });
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancelar", event -> dialog.close());

        VerticalLayout body = new VerticalLayout(rating, comment, code);
        body.addClassName("rs-gap");
        dialog.add(body);
        dialog.getFooter().add(cancel, submit);
        dialog.open();
    }

    private Div reviewRow(ReviewDto review) {
        Div row = new Div();
        row.addClassName("review-row");

        HorizontalLayout head = new HorizontalLayout();
        head.addClassName("rs-items-center");
        Span stars = new Span("★".repeat(Math.max(0, review.rating())) + "☆".repeat(Math.max(0, 5 - review.rating())));
        stars.addClassName("rating-stars");
        Span who = new Span(display(review.studentDisplayName(), review.studentUsername())
                + " · " + TimeFormat.relative(review.createdAt()));
        who.addClassName("post-handle");
        head.add(stars, who);

        row.add(head);
        if (review.comment() != null && !review.comment().isBlank()) {
            Span text = new Span(review.comment());
            text.addClassName("review-text");
            row.add(text);
        }
        return row;
    }

    // ============================ Helpers ============================

    private VerticalLayout card(String title, com.vaadin.flow.component.Component... children) {
        VerticalLayout card = new VerticalLayout();
        card.addClassName("prepa-card");
        Span heading = new Span(title);
        heading.addClassName("prepa-card-title");
        card.add(heading);
        for (com.vaadin.flow.component.Component child : children) {
            card.add(child);
        }
        return card;
    }

    private Div emptyState(VaadinIcon icon, String title, String subtitle) {
        Div div = new Div();
        div.addClassName("empty-state");
        Span iconSpan = new Span(icon.create());
        iconSpan.addClassName("empty-icon");
        Span titleSpan = new Span(title);
        titleSpan.addClassName("empty-state-title");
        div.add(iconSpan, titleSpan, new Span(subtitle));
        return div;
    }

    private String ratingText(double average, long count) {
        if (count <= 0) {
            return "Sin reseñas todavía";
        }
        return String.format(java.util.Locale.forLanguageTag("es"), "%.1f ★ · %d reseña%s",
                average, count, count == 1 ? "" : "s");
    }

    private String money(BigDecimal value) {
        if (value == null) {
            return "$0";
        }
        return "$" + value.stripTrailingZeros().toPlainString();
    }

    private String statusLabel(ConnectionStatus status) {
        return switch (status) {
            case PENDING -> "Pendiente";
            case ACCEPTED -> "Conectado";
            case REJECTED -> "Rechazada";
        };
    }

    private String display(String displayName, String username) {
        return displayName == null || displayName.isBlank() ? username : displayName;
    }
}
