package com.redsocial.ui.view;

import com.redsocial.common.dto.CreateProjectRequest;
import com.redsocial.common.dto.ProjectDto;
import com.redsocial.common.dto.ProjectKind;
import com.redsocial.common.dto.ProjectStatus;
import com.redsocial.ui.client.ApiException;
import com.redsocial.ui.client.ApiService;
import com.redsocial.ui.session.UserSession;
import com.redsocial.ui.util.AvatarUtil;
import com.redsocial.ui.util.Toast;
import com.redsocial.ui.util.TimeFormat;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Route(value = "projects", layout = MainLayout.class)
@PageTitle("Proyectos | RED-SOCIAL")
public class ProjectsView extends ProtectedView {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d", Locale.forLanguageTag("es"));
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("es"));
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("es"));

    private final ApiService api;
    private final UserSession session;
    private final VerticalLayout content = new VerticalLayout();
    private final Tabs tabs = new Tabs();
    private final Tab tabEvents = new Tab("Eventos");
    private final Tab tabDonations = new Tab("Donaciones");
    private final Tab tabProjects = new Tab("Proyectos");
    private final Button create = new Button("Nuevo", VaadinIcon.PLUS.create(), event -> createDialog());

    public ProjectsView(ApiService api, UserSession session) {
        super(session);
        this.api = api;
        this.session = session;

        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        create.addClassName("projects-create");

        add(header(), create, buildTabs(), content);
        tabs.addSelectedChangeListener(event -> load());
        tabs.setSelectedTab(tabEvents);
    }

    private Div header() {
        Div header = new Div();
        header.addClassName("page-header");
        Span eyebrow = new Span("COMUNIDAD");
        eyebrow.addClassName("eyebrow");
        Span title = new Span("Proyectos");
        title.addClassName("page-title");
        header.add(eyebrow, title);
        return header;
    }

    private Tabs buildTabs() {
        tabs.add(tabEvents, tabDonations, tabProjects);
        tabs.setWidthFull();
        tabs.addClassName("projects-tabs");
        return tabs;
    }

    @Override
    public void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        load();
    }

    private void load() {
        loadKind(selectedKind(), null);
    }

    private ProjectKind selectedKind() {
        Tab selected = tabs.getSelectedTab();
        if (selected == tabDonations) {
            return ProjectKind.DONATION;
        }
        return selected == tabProjects ? ProjectKind.PROJECT : ProjectKind.EVENT;
    }

    private void loadKind(ProjectKind kind, ProjectStatus status) {
        content.removeAll();
        for (int i = 0; i < 3; i++) {
            content.add(com.redsocial.ui.component.Skeleton.card());
        }
        try {
            List<ProjectDto> projects = api.projects(kind, status);
            if (projects.isEmpty()) {
                content.add(emptyState(kind));
                return;
            }
            for (ProjectDto project : projects) {
                content.add(render(project, kind));
            }
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private Div render(ProjectDto project, ProjectKind kind) {
        Div card = new Div();
        card.addClassName("project-card");
        card.addClassName("project-" + kind.name().toLowerCase());

        card.add(buildCreatorHead(project));

        Span title = new Span(project.title());
        title.addClassName("project-title");
        Span description = new Span(project.description());
        description.addClassName("project-description");
        card.add(title, description);

        switch (kind) {
            case EVENT -> {
                HorizontalLayout meta = new HorizontalLayout(dateBadge(project), new Span(formatEvent(project)),
                        new Span(project.location() == null ? "" : project.location()));
                meta.addClassName("project-meta");
                meta.setAlignItems(FlexComponent.Alignment.CENTER);
                card.add(meta);
            }
            case DONATION -> card.add(donationBlock(project));
            case PROJECT -> {
                Span badge = new Span(statusLabel(project.status()));
                badge.addClassName("project-status");
                badge.addClassName("status-" + project.status().name().toLowerCase());
                card.add(badge);
            }
        }

        if (isMine(project)) {
            Button delete = new Button(VaadinIcon.TRASH.create(),
                    event -> deleteProject(project));
            delete.addClassName("project-delete");
            delete.addClassName("action-btn");
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
            card.add(delete);
        }
        return card;
    }

    private boolean isMine(ProjectDto project) {
        return session.getUser() != null && Objects.equals(project.creatorId(), session.getUser().id());
    }

    private void deleteProject(ProjectDto project) {
        try {
            api.deleteProject(project.id());
            Toast.show("Proyecto eliminado");
            loadKind(selectedKind(), null);
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private HorizontalLayout buildCreatorHead(ProjectDto project) {
        Avatar avatar = AvatarUtil.create(
                project.creatorDisplayName(), project.creatorUsername(), project.creatorAvatarUrl());
        avatar.addClassName("rs-avatar-sm");

        Span author = new Span(project.creatorDisplayName() == null
                ? project.creatorUsername()
                : project.creatorDisplayName());
        author.addClassName("post-author");
        author.addClickListener(event -> UI.getCurrent().navigate("profile/" + project.creatorUsername()));

        Span handle = new Span("@" + project.creatorUsername() + " · " + TimeFormat.relative(project.createdAt()));
        handle.addClassName("post-handle");

        VerticalLayout info = new VerticalLayout(author, handle);
        info.setPadding(false);
        info.setSpacing(false);

        HorizontalLayout head = new HorizontalLayout(avatar, info);
        head.setPadding(false);
        head.setSpacing(true);
        head.setWidthFull();
        head.setAlignItems(FlexComponent.Alignment.CENTER);
        head.addClassName("project-head");
        return head;
    }

    private Div dateBadge(ProjectDto project) {
        Div badge = new Div();
        badge.addClassName("date-badge");
        Span day = new Span(project.eventDate() == null ? "?" : DAY.format(project.eventDate()));
        day.addClassName("date-badge-day");
        Span month = new Span(project.eventDate() == null ? "TBA" : MONTH.format(project.eventDate()).toUpperCase());
        month.addClassName("date-badge-month");
        badge.add(day, month);
        return badge;
    }

    private String formatEvent(ProjectDto project) {
        if (project.eventDate() == null) {
            return "Fecha a confirmar";
        }
        return TIME.format(project.eventDate());
    }

    private VerticalLayout donationBlock(ProjectDto project) {
        VerticalLayout block = new VerticalLayout();
        block.setPadding(false);
        block.setSpacing(true);
        block.setWidthFull();

        BigDecimal target = project.targetAmount() == null ? BigDecimal.ZERO : project.targetAmount();
        BigDecimal raised = project.raisedAmount() == null ? BigDecimal.ZERO : project.raisedAmount();
        double percent = target.signum() > 0
                ? raised.divide(target, 4, java.math.RoundingMode.HALF_UP).doubleValue() : 0;
        double capped = Math.min(1.0, Math.max(0, percent));

        ProgressBar bar = new ProgressBar(0, 1, capped);
        bar.setWidthFull();
        bar.addClassName("project-progress");

        Span counter = new Span(formatMoney(raised) + " de " + formatMoney(target)
                + " · " + project.donationCount() + " aportes");
        counter.addClassName("project-counter");

        Button donate = new Button("Donar", VaadinIcon.HEART.create(), event -> donateDialog(project));
        donate.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        donate.addClassName("project-donate");

        HorizontalLayout bottom = new HorizontalLayout(counter, donate);
        bottom.setWidthFull();
        bottom.setAlignItems(FlexComponent.Alignment.CENTER);
        bottom.setFlexGrow(1, counter);

        block.add(bar, bottom);
        return block;
    }

    private void donateDialog(ProjectDto project) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Aportar a «" + project.title() + "»");
        dialog.setWidth("400px");

        NumberField amount = new NumberField("Monto");
        amount.setPlaceholder("Ej: 5000");
        amount.setMin(0.01);
        amount.setValue(1000.0);
        amount.setPrefixComponent(VaadinIcon.MONEY.create());
        amount.setWidthFull();

        Button confirm = new Button("Aportar", event -> {
            try {
                Double value = amount.getValue();
                if (value == null || value <= 0) {
                    Toast.show("Ingresá un monto mayor a cero");
                    return;
                }
                api.donate(project.id(), BigDecimal.valueOf(value));
                dialog.close();
                Toast.show("¡Gracias por tu aporte!");
                loadKind(ProjectKind.DONATION, null);
            } catch (ApiException ex) {
                Toast.show(ex.getMessage());
            }
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancelar", event -> dialog.close());

        VerticalLayout body = new VerticalLayout(amount);
        body.setPadding(false);
        dialog.add(body);
        dialog.getFooter().add(cancel, confirm);
        dialog.open();
    }

    private void createDialog() {
        switch (selectedKind()) {
            case EVENT -> createEventDialog();
            case DONATION -> createDonationDialog();
            default -> createProjectDialog();
        }
    }

    private void createEventDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nuevo evento");
        dialog.setWidth("540px");

        TextField title = new TextField("Título");
        title.setWidthFull();
        title.setPlaceholder("Ej: Feria de Ciencias");

        TextArea description = new TextArea("Descripción");
        description.setWidthFull();
        description.setMaxLength(4000);
        description.setPlaceholder("Contá de qué se trata el evento.");

        DateTimePicker eventDate = new DateTimePicker("Fecha y hora");
        eventDate.setWidthFull();
        eventDate.setDatePlaceholder("dd/mm/aaaa");
        eventDate.setTimePlaceholder("hh:mm");

        TextField location = new TextField("Ubicación");
        location.setWidthFull();
        location.setPlaceholder("Ej: Aula Magna, Edificio Central");

        VerticalLayout body = new VerticalLayout(title, description, eventDate, location);
        body.setPadding(false);
        body.setSpacing(true);

        Button save = new Button("Guardar", event -> save(ProjectKind.EVENT, title, description,
                new CreateProjectRequest(ProjectKind.EVENT, null, null, eventDate.getValue(), location.getValue(),
                        null, null), dialog));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancelar", event -> dialog.close());
        dialog.add(body);
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void createDonationDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nueva campaña de donación");
        dialog.setWidth("540px");

        TextField title = new TextField("Título");
        title.setWidthFull();
        title.setPlaceholder("Ej: Espacio de Impresión 3D");

        TextArea description = new TextArea("Descripción");
        description.setWidthFull();
        description.setMaxLength(4000);
        description.setPlaceholder("Contá para qué espacio universitario es la campaña.");

        NumberField target = new NumberField("Meta de recaudación ($)");
        target.setWidthFull();
        target.setMin(1);
        target.setPrefixComponent(VaadinIcon.MONEY.create());

        VerticalLayout body = new VerticalLayout(title, description, target);
        body.setPadding(false);
        body.setSpacing(true);

        Button save = new Button("Guardar", event -> save(ProjectKind.DONATION, title, description,
                new CreateProjectRequest(ProjectKind.DONATION, null, null, null, null,
                        target.getValue() != null ? BigDecimal.valueOf(target.getValue()) : null, null), dialog));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancelar", event -> dialog.close());
        dialog.add(body);
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void createProjectDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nuevo proyecto");
        dialog.setWidth("540px");

        TextField title = new TextField("Título");
        title.setWidthFull();
        title.setPlaceholder("Ej: Wi-Fi académico gratuito");

        TextArea description = new TextArea("Descripción");
        description.setWidthFull();
        description.setMaxLength(4000);
        description.setPlaceholder("Contá qué mejora proponés para la institución.");

        Select<ProjectStatus> status = new Select<>();
        status.setLabel("Estado");
        status.setItems(ProjectStatus.values());
        status.setItemLabelGenerator(ProjectsView::statusLabel);
        status.setValue(ProjectStatus.PROPOSED);
        status.setWidthFull();

        VerticalLayout body = new VerticalLayout(title, description, status);
        body.setPadding(false);
        body.setSpacing(true);

        Button save = new Button("Guardar", event -> save(ProjectKind.PROJECT, title, description,
                new CreateProjectRequest(ProjectKind.PROJECT, null, null, null, null, null, status.getValue()), dialog));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancelar", event -> dialog.close());
        dialog.add(body);
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void save(ProjectKind kind, TextField title, TextArea description,
                      CreateProjectRequest request, Dialog dialog) {
        try {
            if (title.getValue() == null || title.getValue().isBlank()) {
                Toast.show("El título es obligatorio");
                return;
            }
            if (description.getValue() == null || description.getValue().isBlank()) {
                Toast.show("La descripción es obligatoria");
                return;
            }
            CreateProjectRequest finalRequest = new CreateProjectRequest(
                    kind,
                    title.getValue().trim(),
                    description.getValue().trim(),
                    request.eventDate(),
                    request.location(),
                    request.targetAmount(),
                    request.status());
            api.createProject(finalRequest);
            dialog.close();
            Toast.show(kindLabel(kind) + " creado");
            loadKind(selectedKind(), null);
        } catch (ApiException ex) {
            Toast.show(ex.getMessage());
        }
    }

    private Div emptyState(ProjectKind kind) {
        Div div = new Div();
        div.addClassName("empty-state");
        Span icon = new Span(new com.vaadin.flow.component.icon.Icon(switch (kind) {
            case EVENT -> com.vaadin.flow.component.icon.VaadinIcon.CALENDAR;
            case DONATION -> com.vaadin.flow.component.icon.VaadinIcon.HEART;
            default -> com.vaadin.flow.component.icon.VaadinIcon.ROCKET;
        }));
        icon.addClassName("empty-icon");
        Span title = new Span(switch (kind) {
            case EVENT -> "Todavía no hay eventos";
            case DONATION -> "No hay campañas de donación";
            default -> "No hay proyectos cargados";
        });
        title.addClassName("empty-state-title");
        Span subtitle = new Span(switch (kind) {
            case EVENT -> "Cargá el primer evento del campus.";
            case DONATION -> "Iniciá una campaña para un nuevo espacio universitario.";
            default -> "Sumá un proyecto para mejorar la institución.";
        });
        div.add(icon, title, subtitle);
        div.setWidthFull();
        return div;
    }

    private static String kindLabel(ProjectKind kind) {
        return switch (kind) {
            case EVENT -> "Nuevo evento";
            case DONATION -> "Nueva campaña de donación";
            default -> "Nuevo proyecto";
        };
    }

    private static String statusLabel(ProjectStatus status) {
        return switch (status) {
            case PROPOSED -> "Propuesto";
            case IN_PROGRESS -> "En curso";
            case COMPLETED -> "Completado";
        };
    }

    private static String formatMoney(BigDecimal value) {
        return "$ " + value.longValue();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        super.beforeEnter(event);
    }
}