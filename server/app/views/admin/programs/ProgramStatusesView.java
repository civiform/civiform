package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.admin.routes;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import play.twirl.api.Content;
import services.DateConverter;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.Modal;
import views.components.Modal.Width;
import views.style.AdminStyles;
import views.style.StyleUtils;
import views.style.Styles;

public final class ProgramStatusesView extends BaseHtmlView {
  private final AdminLayout layout;
  private final DateConverter dateConverter;

  @Inject
  public ProgramStatusesView(AdminLayoutFactory layoutFactory, DateConverter dateConverter) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.dateConverter = checkNotNull(dateConverter);
  }

  public Content render(ProgramDefinition program) {
    // TODO(#2752): Use real statuses from the program.
    ImmutableList<ApplicationStatus> actualStatuses =
        ImmutableList.of(
            ApplicationStatus.create("Approved", Instant.now(), "Some email"),
            ApplicationStatus.create("Denied", Instant.now(), ""),
            ApplicationStatus.create("Needs more information", Instant.now(), ""));

    Modal createStatusModal = makeStatusModal(Optional.empty());
    ButtonTag createStatusTriggerButton =
        makeSvgTextButton("Create a new status", Icons.PLUS)
            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, "my-2")
            .withId(createStatusModal.getTriggerButtonId());

    Pair<DivTag, ImmutableList<Modal>> statusContainerAndModals =
        renderStatusContainer(actualStatuses);

    DivTag contentDiv =
        div()
            .withClasses("px-4")
            .with(
                div()
                    .withClasses(
                        "flex",
                        "items-center",
                        "space-x-4",
                        "mt-12",
                        "mb-10")
                    .with(
                        h1(
                            String.format(
                                "Manage application statuses for %s", program.adminName())),
                        div().withClass("flex-grow"),
                        renderManageTranslationsLink(program),
                        createStatusTriggerButton),
                statusContainerAndModals.getLeft());

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle("Manage program statuses")
            .addMainContent(contentDiv)
            .addModals(createStatusModal)
            .addModals(statusContainerAndModals.getRight());

    return layout.renderCentered(htmlBundle);
  }

  private ButtonTag renderManageTranslationsLink(ProgramDefinition program) {
    String linkDestination =
        routes.AdminProgramTranslationsController.edit(
                program.id(), LocalizedStrings.DEFAULT_LOCALE.toLanguageTag())
            .url();
    ButtonTag button =
        makeSvgTextButton("Manage translations", Icons.LANGUAGE)
            .withClass(AdminStyles.SECONDARY_BUTTON_STYLES);
    return asRedirectButton(button, linkDestination);
  }

  private Pair<DivTag, ImmutableList<Modal>> renderStatusContainer(
      ImmutableList<ApplicationStatus> statuses) {
    String numResultsText =
        statuses.size() == 1 ? "1 result" : String.format("%d results", statuses.size());
    ImmutableList<Pair<DivTag, Modal>> statusTagsAndModals =
        statuses.stream().map(s -> renderStatusItem(s)).collect(ImmutableList.toImmutableList());
    return Pair.of(
        div()
            .with(
                p(numResultsText),
                div()
                    .withClasses("mt-6", "border", "rounded-md", "divide-y")
                    .with(statusTagsAndModals.stream().map(Pair::getLeft))
                    .condWith(
                        statuses.isEmpty(),
                        p("No statuses have been created yet")
                            .withClasses("ml-4", "my-4"))),
        statusTagsAndModals.stream().map(Pair::getRight).collect(ImmutableList.toImmutableList()));
  }

  private Pair<DivTag, Modal> renderStatusItem(ApplicationStatus status) {
    Modal editStatusModal = makeStatusModal(Optional.of(status));
    ButtonTag editStatusTriggerButton =
        makeSvgTextButton("Edit", Icons.EDIT)
            .withClass(AdminStyles.TERTIARY_BUTTON_STYLES)
            .withId(editStatusModal.getTriggerButtonId());
    return Pair.of(
        div()
            .withClasses(
                "pl-7",
                "pr-6",
                "py-9",
                "font-normal",
                "space-x-2",
                "flex",
                "items-center",
                StyleUtils.hover("bg-gray-100"))
            .with(
                div()
                    .withClass("w-1/4")
                    .with(
                        // TODO(#2752): Optional SVG icon for status attribute.
                        span(status.statusName()).withClasses("ml-2", "break-words")),
                div()
                    .with(
                        p().withClass("text-sm")
                            .with(
                                span("Edited on "),
                                span(dateConverter.renderDate(status.lastEdited()))
                                    .withClass("font-semibold")))
                    .condWith(
                        !status.emailContent().isEmpty(),
                        p().withClasses(
                                "mt-1", "text-xs", "flex", "items-center")
                            .with(
                                Icons.svg(Icons.EMAIL, 22)
                                    // TODO(#2752): Once SVG icon sizes are consistent, just set
                                    // size to 18.
                                    .withWidth("18")
                                    .withHeight("18")
                                    .withClasses("mr-2", "inline-block"),
                                span("Applicant notification email added"))),
                div().withClass("flex-grow"),
                makeSvgTextButton("Delete", Icons.DELETE)
                    .withClass(AdminStyles.TERTIARY_BUTTON_STYLES),
                editStatusTriggerButton),
        editStatusModal);
  }

  private Modal makeStatusModal(Optional<ApplicationStatus> status) {
    FormTag content =
        form()
            .withClasses("px-6", "py-2")
            .with(
                FieldWithLabel.input()
                    .setLabelText("Status name (required)")
                    // TODO(#2752): Potentially move placeholder text to an actual
                    // description.
                    .setPlaceholderText("Enter status name here")
                    .setValue(status.map(ApplicationStatus::statusName))
                    .getInputTag(),
                div()
                    .withClasses("pt-8")
                    .with(
                        FieldWithLabel.textArea()
                            .setLabelText("Applicant status change email")
                            .setPlaceholderText("Notify the Applicant about the status change")
                            .setRows(OptionalLong.of(5))
                            .setValue(status.map(ApplicationStatus::emailContent))
                            .getTextareaTag()),
                div()
                    .withClasses("flex", "mt-5", "space-x-2")
                    .with(
                        div().withClass("flex-grow"),
                        // TODO(#2752): Add a cancel button that clears state.
                        submitButton("Confirm").withClass(AdminStyles.TERTIARY_BUTTON_STYLES)));
    // We prepend a "a-" since element IDs must start with an alphabetic character, whereas UUIDs
    // can start with a numeric character.
    String modalId = "a-" + UUID.randomUUID().toString();
    return Modal.builder(modalId, content)
        .setModalTitle(status.isPresent() ? "Edit this status" : "Create a new status")
        .setWidth(Width.HALF)
        .build();
  }

  // TODO(#2752): Use a domain-specific representation of an ApplicationStatus
  // rather than an auto-value.
  @AutoValue
  abstract static class ApplicationStatus {

    static ApplicationStatus create(String statusName, Instant lastEdited, String emailContent) {
      return new AutoValue_ProgramStatusesView_ApplicationStatus(
          statusName, lastEdited, emailContent);
    }

    abstract String statusName();

    abstract Instant lastEdited();

    abstract String emailContent();
  }
}
