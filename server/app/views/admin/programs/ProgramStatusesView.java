package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.admin.routes;
import forms.admin.ProgramStatusesForm;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import play.data.Form;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.twirl.api.Content;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.StatusDefinitions;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.Modal;
import views.components.Modal.Width;
import views.components.ToastMessage;
import views.style.AdminStyles;
import views.style.StyleUtils;
import views.style.Styles;

public final class ProgramStatusesView extends BaseHtmlView {
  public static final String DELETE_STATUS_TEXT_NAME = "deleteStatusText";

  private final AdminLayout layout;
  private final MessagesApi messagesApi;

  @Inject
  public ProgramStatusesView(AdminLayoutFactory layoutFactory, MessagesApi messagesApi) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.messagesApi = checkNotNull(messagesApi);
  }

  public Content render(
      Http.Request request,
      ProgramDefinition program,
      Optional<Form<ProgramStatusesForm>> maybeStatusForm) {
    Modal createStatusModal =
        makeStatusEditModal(
            request,
            program,
            Optional.empty(),
            formForCurrentStatus(Optional.empty(), maybeStatusForm));
    ButtonTag createStatusTriggerButton =
        makeSvgTextButton("Create a new status", Icons.PLUS)
            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, Styles.MY_2)
            .withId(createStatusModal.getTriggerButtonId());

    Pair<DivTag, ImmutableList<Modal>> statusContainerAndModals =
        renderStatusContainer(request, program, maybeStatusForm);

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

    Http.Flash flash = request.flash();
    if (flash.get("error").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.error(flash.get("error").get()).setDuration(-1));
    } else if (flash.get("success").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.success(flash.get("success").get()).setDuration(-1));
    }

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
      Http.Request request,
      ProgramDefinition program,
      Optional<Form<ProgramStatusesForm>> maybeEditForm) {
    ImmutableList<StatusDefinitions.Status> statuses = program.statusDefinitions().getStatuses();
    String numResultsText =
        statuses.size() == 1 ? "1 result" : String.format("%d results", statuses.size());
    ImmutableList<Pair<DivTag, ImmutableList<Modal>>> statusTagsAndModals =
        statuses.stream()
            .map(
                s -> {
                  return renderStatusItem(
                      request, program, s, formForCurrentStatus(Optional.of(s), maybeEditForm));
                })
            .collect(ImmutableList.toImmutableList());
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
        statusTagsAndModals.stream()
            .map(Pair::getRight)
            .flatMap(Collection::stream)
            .collect(ImmutableList.toImmutableList()));
  }

  private Pair<DivTag, ImmutableList<Modal>> renderStatusItem(
      Http.Request request,
      ProgramDefinition program,
      StatusDefinitions.Status status,
      Optional<Form<ProgramStatusesForm>> maybeEditForm) {
    Modal editStatusModal =
        makeStatusEditModal(request, program, Optional.of(status), maybeEditForm);
    ButtonTag editStatusTriggerButton =
        makeSvgTextButton("Edit", Icons.EDIT)
            .withClass(AdminStyles.TERTIARY_BUTTON_STYLES)
            .withId(editStatusModal.getTriggerButtonId());

    Modal deleteStatusModal = makeStatusDeleteModal(request, program, status.statusText());
    ButtonTag deleteStatusTriggerButton =
        makeSvgTextButton("Delete", Icons.DELETE)
            .withClass(AdminStyles.TERTIARY_BUTTON_STYLES)
            .withId(deleteStatusModal.getTriggerButtonId());
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
                        span(status.statusText()).withClasses(Styles.ML_2, Styles.BREAK_WORDS)),
                div()
                    .condWith(
                        status.emailBodyText().isPresent(),
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
                deleteStatusTriggerButton,
                editStatusTriggerButton),
        ImmutableList.of(editStatusModal, deleteStatusModal));
  }

  private Modal makeStatusDeleteModal(
      Http.Request request, ProgramDefinition program, String toDeleteStatusText) {
    DivTag content =
        div()
            .withClasses(Styles.PX_6, Styles.PY_2)
            .with(
                p(
                    "Warning: This will also remove any translated content for the status and"
                        + " email body."),
                form()
                    .withMethod("POST")
                    .withAction(routes.AdminProgramStatusesController.delete(program.id()).url())
                    .with(
                        makeCsrfTokenInputTag(request),
                        input()
                            .isHidden()
                            .withName(DELETE_STATUS_TEXT_NAME)
                            .withValue(toDeleteStatusText),
                        div()
                            .withClasses(Styles.FLEX, Styles.MT_5, Styles.SPACE_X_2)
                            .with(
                                div().withClass(Styles.FLEX_GROW),
                                submitButton("Delete")
                                    .withClass(AdminStyles.SECONDARY_BUTTON_STYLES))));

    return Modal.builder(randomModalId(), content).setModalTitle("Delete this status").build();
  }

  private static String randomModalId() {
    // We prepend a "a-" since element IDs must start with an alphabetic character, whereas UUIDs
    // can start with a numeric character.
    return "a-" + UUID.randomUUID().toString();
  }

  /**
   * Returns the form if it matches the current status object being rendered. The page has multiple
   * inline modals for editing and creating statuses.
   */
  private Optional<Form<ProgramStatusesForm>> formForCurrentStatus(
      Optional<StatusDefinitions.Status> maybeStatus,
      Optional<Form<ProgramStatusesForm>> maybeEditForm) {
    String wantconfiguredStatusText =
        maybeStatus.map(StatusDefinitions.Status::statusText).orElse("");
    return maybeEditForm.isPresent()
            && wantconfiguredStatusText.equalsIgnoreCase(
                maybeEditForm.get().value().get().getconfiguredStatusText())
        ? maybeEditForm
        : Optional.empty();
  }

  private Modal makeStatusEditModal(
      Http.Request request,
      ProgramDefinition program,
      Optional<StatusDefinitions.Status> maybeStatus,
      Optional<Form<ProgramStatusesForm>> maybeEditForm) {
    // TODO(#2752): Pop the modal open on error on page load.
    String configuredStatusText;
    String statusText;
    String emailBody;
    if (maybeEditForm.isPresent()) {
      ProgramStatusesForm values = maybeEditForm.get().value().get();
      configuredStatusText = values.getconfiguredStatusText();
      statusText = values.getStatusText();
      emailBody = values.getEmailBody();
    } else {
      configuredStatusText = maybeStatus.map(StatusDefinitions.Status::statusText).orElse("");
      statusText = maybeStatus.map(StatusDefinitions.Status::statusText).orElse("");
      emailBody = maybeStatus.map(s -> s.emailBodyText().orElse("")).orElse("");
    }
    Messages messages = messagesApi.preferred(request);

    FormTag content =
        form()
            .withMethod("POST")
            .withAction(routes.AdminProgramStatusesController.createOrUpdate(program.id()).url())
            .withClasses(Styles.PX_6, Styles.PY_2)
            .with(
                makeCsrfTokenInputTag(request),
                input()
                    .isHidden()
                    .withName(ProgramStatusesForm.CONFIGURED_STATUS_TEXT_FORM_NAME)
                    .withValue(configuredStatusText),
                renderFormGlobalErrors(messages, maybeEditForm),
                FieldWithLabel.input()
                    .setFieldName(ProgramStatusesForm.STATUS_TEXT_FORM_NAME)
                    .setLabelText("Status name (required)")
                    // TODO(#2617): Potentially move placeholder text to an actual
                    // description.
                    .setPlaceholderText("Enter status name here")
                    .setValue(statusText)
                    .setFieldErrors(
                        messages,
                        maybeEditForm
                            .map(f -> f.errors(ProgramStatusesForm.STATUS_TEXT_FORM_NAME))
                            .orElse(ImmutableList.of()))
                    .getInputTag(),
                div()
                    .withClasses(Styles.PT_8)
                    .with(
                        FieldWithLabel.textArea()
                            .setFieldName(ProgramStatusesForm.EMAIL_BODY_FORM_NAME)
                            .setLabelText("Applicant status change email")
                            // TODO(#2617): Potentially move placeholder text to an actual
                            // description.
                            .setPlaceholderText("Notify the Applicant about the status change")
                            .setRows(OptionalLong.of(5))
                            .setValue(emailBody)
                            .setFieldErrors(
                                messages,
                                maybeEditForm
                                    .map(f -> f.errors(ProgramStatusesForm.EMAIL_BODY_FORM_NAME))
                                    .orElse(ImmutableList.of()))
                            .getTextareaTag()),
                div()
                    .withClasses(Styles.FLEX, Styles.MT_5, Styles.SPACE_X_2)
                    .with(
                        div().withClass(Styles.FLEX_GROW),
                        // TODO(#2752): Add a cancel button that clears state.
                        submitButton("Confirm").withClass(AdminStyles.TERTIARY_BUTTON_STYLES)));
    return Modal.builder(randomModalId(), content)
        .setModalTitle(configuredStatusText.isEmpty() ? "Create a new status" : "Edit this status")
        .setWidth(Width.HALF)
        .build();
  }

  private DivTag renderFormGlobalErrors(
      Messages messages, Optional<Form<ProgramStatusesForm>> maybeEditForm) {
    ImmutableList<String> errors =
        maybeEditForm.map(Form::globalErrors).orElse(ImmutableList.of()).stream()
            .map(e -> e.format(messages))
            .collect(ImmutableList.toImmutableList());
    return errors.isEmpty()
        ? div()
        : div(each(errors, e -> p(e).withClasses(Styles.TEXT_SM, Styles.TEXT_RED_600)))
            .withClasses(Styles.PB_4);
  }
}
