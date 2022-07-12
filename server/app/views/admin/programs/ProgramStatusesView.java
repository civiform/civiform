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
    // TODO(#2752): Use real statuses from the program. Also may be able
    // to do away with the AutoValue below if this information is encoded elsewhere.
    ImmutableList<ApplicationStatus> actualStatuses =
        ImmutableList.of(
            ApplicationStatus.create("Approved", Instant.now(), "Some email"),
            ApplicationStatus.create("Denied", Instant.now(), ""),
            ApplicationStatus.create("Needs more information", Instant.now(), ""));

    Modal createStatusModal = makeStatusModal(Optional.empty());
    ButtonTag createStatusTriggerButton =
        makeSvgTextButton("Create a new status", Icons.PLUS)
            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, Styles.MY_2)
            .withId(createStatusModal.getTriggerButtonId());

    Pair<DivTag, ImmutableList<Modal>> statusContainerAndModals =
        renderStatusContainer(actualStatuses);

    DivTag contentDiv =
        div()
            .withClasses(Styles.PX_4)
            .with(
                div()
                    .withClasses(
                        Styles.FLEX,
                        Styles.ITEMS_CENTER,
                        Styles.SPACE_X_4,
                        Styles.MT_12,
                        Styles.MB_10)
                    .with(
                        h1(
                            String.format(
                                "Manage application statuses for %s", program.adminName())),
                        div().withClass(Styles.FLEX_GROW),
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
                    .withClasses(Styles.MT_6, Styles.BORDER, Styles.ROUNDED_MD, Styles.DIVIDE_Y)
                    .with(statusTagsAndModals.stream().map(Pair::getLeft))
                    .condWith(
                        statuses.isEmpty(),
                        p("No statuses have been created yet")
                            .withClasses(Styles.ML_4, Styles.MY_4))),
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
                Styles.PL_7,
                Styles.PR_6,
                Styles.PY_9,
                Styles.FONT_NORMAL,
                Styles.SPACE_X_2,
                Styles.FLEX,
                Styles.ITEMS_CENTER,
                StyleUtils.hover(Styles.BG_GRAY_100))
            .with(
                div()
                    .withClass(Styles.W_1_4)
                    .with(
                        // TODO(#2752): Optional SVG icon for status attribute.
                        span(status.statusName()).withClasses(Styles.ML_2, Styles.BREAK_WORDS)),
                div()
                    .with(
                        p().withClass(Styles.TEXT_SM)
                            .with(
                                span("Edited on "),
                                span(dateConverter.renderDate(status.lastEdited()))
                                    .withClass(Styles.FONT_SEMIBOLD)))
                    .condWith(
                        !status.emailContent().isEmpty(),
                        p().withClasses(
                                Styles.MT_1, Styles.TEXT_XS, Styles.FLEX, Styles.ITEMS_CENTER)
                            .with(
                                Icons.svg(Icons.EMAIL, 22)
                                    // TODO(#2752): Once SVG icon sizes are consistent, just set
                                    // size to 18.
                                    .withWidth("18")
                                    .withHeight("18")
                                    .withClasses(Styles.MR_2, Styles.INLINE_BLOCK),
                                span("Applicant notification email added"))),
                div().withClass(Styles.FLEX_GROW),
                makeSvgTextButton("Delete", Icons.DELETE)
                    .withClass(AdminStyles.TERTIARY_BUTTON_STYLES),
                editStatusTriggerButton),
        editStatusModal);
  }

  private Modal makeStatusModal(Optional<ApplicationStatus> status) {
    FormTag content =
        form()
            .withClasses(Styles.PX_6, Styles.PY_2)
            .with(
                FieldWithLabel.input()
                    .setLabelText("Status name (required)")
                    // TODO(#2752): Potentially move placeholder text to an actual
                    // description.
                    .setPlaceholderText("Enter status name here")
                    .setValue(status.map(ApplicationStatus::statusName))
                    .getInputTag(),
                div()
                    .withClasses(Styles.PT_8)
                    .with(
                        FieldWithLabel.textArea()
                            .setLabelText("Applicant status change email")
                            .setPlaceholderText("Notify the Applicant about the status change")
                            .setRows(OptionalLong.of(5))
                            .setValue(status.map(ApplicationStatus::emailContent))
                            .getTextareaTag()),
                div()
                    .withClasses(Styles.FLEX, Styles.MT_5, Styles.SPACE_X_2)
                    .with(
                        div().withClass(Styles.FLEX_GROW),
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
