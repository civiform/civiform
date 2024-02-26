package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.admin.routes;
import forms.admin.ProgramStatusesForm;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.LabelTag;
import j2html.tags.specialized.PTag;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalLong;
import org.apache.commons.lang3.tuple.Pair;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import services.program.StatusDefinitions;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.Modal;
import views.components.Modal.Width;
import views.components.ToastMessage;
import views.style.BaseStyles;
import views.style.ReferenceClasses;

public final class ProgramStatusesView extends BaseHtmlView {
  public static final String DELETE_STATUS_TEXT_NAME = "deleteStatusText";

  private final AdminLayout layout;
  private final FormFactory formFactory;
  private final MessagesApi messagesApi;

  @Inject
  public ProgramStatusesView(
      AdminLayoutFactory layoutFactory, FormFactory formFactory, MessagesApi messagesApi) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.formFactory = formFactory;
    this.messagesApi = checkNotNull(messagesApi);
  }

  /**
   * Renders a list of program statuses along with modal create / edit / delete forms.
   *
   * @param request The associated request.
   * @param program The program to render program statuses for.
   * @param maybeStatusForm Set if the form is being rendered in response to an attempt to create /
   *     edit a program status. Note that while the view itself may render multiple program
   *     statuses, the provided form will correspond to creating / editing a single program status.
   */
  public Content render(
      Http.Request request,
      ProgramDefinition program,
      Optional<Form<ProgramStatusesForm>> maybeStatusForm) {
    final boolean displayOnLoad;
    final Form<ProgramStatusesForm> createStatusForm;
    if (isCreationForm(maybeStatusForm)) {
      createStatusForm = maybeStatusForm.get();
      displayOnLoad = true;
    } else {
      createStatusForm =
          formFactory.form(ProgramStatusesForm.class).fill(new ProgramStatusesForm());
      displayOnLoad = false;
    }
    Modal createStatusModal =
        makeStatusUpdateModal(
            request,
            program,
            createStatusForm,
            displayOnLoad,
            /* showEmailDeletionWarning= */ false);
    ButtonTag createStatusTriggerButton =
        makeSvgTextButton("Create a new status", Icons.PLUS)
            .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON, "my-2")
            .withId(createStatusModal.getTriggerButtonId());

    Pair<DivTag, ImmutableList<Modal>> statusContainerAndModals =
        renderProgramStatusesContainer(request, program, maybeStatusForm);

    DivTag topBarDiv =
        div()
            .withClasses("flex", "items-center", "space-x-4", "mt-12", "mb-10")
            .with(
                h1(
                    String.format(
                        "Manage application statuses for %s",
                        program.localizedName().getDefault())),
                div().withClass("flex-grow"));

    Optional<ButtonTag> maybeManageTranslationsLink = renderManageTranslationsLink(program);
    if (maybeManageTranslationsLink.isPresent()) {
      topBarDiv.with(maybeManageTranslationsLink.get());
    }
    topBarDiv.with(createStatusTriggerButton);

    DivTag contentDiv =
        div().withClasses("px-4").with(topBarDiv, statusContainerAndModals.getLeft());

    HtmlBundle htmlBundle =
        layout
            .getBundle(request)
            .setTitle("Manage program statuses")
            .addMainContent(contentDiv)
            .addModals(createStatusModal)
            .addModals(statusContainerAndModals.getRight());

    Http.Flash flash = request.flash();
    if (flash.get("error").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.errorNonLocalized(flash.get("error").get()));
    } else if (flash.get("success").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.success(flash.get("success").get()));
    }

    return layout.renderCentered(htmlBundle);
  }

  private Optional<ButtonTag> renderManageTranslationsLink(ProgramDefinition program) {
    return layout.createManageTranslationsButton(
        program.adminName(),
        /* buttonId= */ Optional.empty(),
        ButtonStyles.OUTLINED_WHITE_WITH_ICON,
        ProgramTranslationReferer.PROGRAM_STATUSES);
  }

  /**
   * Renders the set of program statuses as well as any modals related to editing / deleting the
   * statuses.
   *
   * @param request The associated request.
   * @param program The program to render program statuses for.
   * @param maybeStatusForm Set if the form is being rendered in response to an attempt to create /
   *     edit a program status. Note that while the view itself may render multiple program
   *     statuses, the provided form will correspond to creating / editing a single program status.
   */
  private Pair<DivTag, ImmutableList<Modal>> renderProgramStatusesContainer(
      Http.Request request,
      ProgramDefinition program,
      Optional<Form<ProgramStatusesForm>> maybeStatusForm) {
    ImmutableList<StatusDefinitions.Status> statuses = program.statusDefinitions().getStatuses();
    String numResultsText =
        statuses.size() == 1 ? "1 result" : String.format("%d results", statuses.size());
    ImmutableList<Pair<DivTag, ImmutableList<Modal>>> statusTagsAndModals =
        statuses.stream()
            .map(
                s -> {
                  final Form<ProgramStatusesForm> statusEditForm;
                  final boolean displayEditFormOnLoad;
                  if (isFormForStatus(maybeStatusForm, s)) {
                    statusEditForm = maybeStatusForm.get();
                    displayEditFormOnLoad = true;
                  } else {
                    statusEditForm =
                        formFactory
                            .form(ProgramStatusesForm.class)
                            .fill(ProgramStatusesForm.fromStatus(s));
                    displayEditFormOnLoad = false;
                  }
                  return renderStatusItem(
                      request, program, s, statusEditForm, displayEditFormOnLoad);
                })
            .collect(ImmutableList.toImmutableList());
    // Combine all the DivTags into a rendered list, and collect all Modals into one collection.
    DivTag statusesContainer =
        div()
            .withClass(ReferenceClasses.ADMIN_PROGRAM_STATUS_LIST)
            .with(
                p(numResultsText),
                div()
                    .withClasses("mt-6", "border", "rounded-md", "divide-y")
                    .with(statusTagsAndModals.stream().map(Pair::getLeft))
                    .condWith(
                        statuses.isEmpty(),
                        p("No statuses have been created yet").withClasses("ml-4", "my-4")));
    ImmutableList<Modal> modals =
        statusTagsAndModals.stream()
            .map(Pair::getRight)
            .flatMap(Collection::stream)
            .collect(ImmutableList.toImmutableList());
    return Pair.of(statusesContainer, modals);
  }

  /**
   * Renders a given program status, optionally consulting the request data for values / form
   * errors.
   *
   * @param request The associated request.
   * @param program The program to render program statuses for.
   * @param status The status to render, as read from the existing program definition.
   * @param statusEditForm A form containing the values / validation errors to use when rendering
   *     the status edit form.
   * @param displayOnLoad Whether the edit modal should be displayed on page load.
   */
  private Pair<DivTag, ImmutableList<Modal>> renderStatusItem(
      Http.Request request,
      ProgramDefinition program,
      StatusDefinitions.Status status,
      Form<ProgramStatusesForm> statusEditForm,
      boolean displayOnLoad) {
    Modal editStatusModal =
        makeStatusUpdateModal(
            request,
            program,
            statusEditForm,
            displayOnLoad,
            /* showEmailDeletionWarning= */ status.localizedEmailBodyText().isPresent());
    ButtonTag editStatusTriggerButton =
        makeSvgTextButton("Edit", Icons.EDIT)
            .withClass(ButtonStyles.CLEAR_WITH_ICON)
            .withId(editStatusModal.getTriggerButtonId());

    Modal deleteStatusModal = makeStatusDeleteModal(request, program, status.statusText());
    ButtonTag deleteStatusTriggerButton =
        makeSvgTextButton("Delete", Icons.DELETE)
            .withClass(ButtonStyles.CLEAR_WITH_ICON)
            .withId(deleteStatusModal.getTriggerButtonId());
    return Pair.of(
        div()
            .withClasses(
                ReferenceClasses.ADMIN_PROGRAM_STATUS_ITEM,
                "pl-7",
                "pr-6",
                "py-9",
                "font-normal",
                "space-x-2",
                "flex",
                "items-center")
            .with(
                div()
                    .withClass("w-1/4")
                    .with(
                        // TODO(#3272): Optional SVG icon for status attribute.
                        span(status.statusText()).withClasses("ml-2", "break-words")),
                div()
                    .condWith(
                        status.localizedEmailBodyText().isPresent(),
                        p().withClasses("mt-1", "text-xs", "flex", "items-center")
                            .with(
                                Icons.svg(Icons.EMAIL)
                                    // 4.5 is 18px as defined in tailwind.config.js
                                    .withClasses("mr-2", "inline-block", "h-4.5", "w-4.5"),
                                span("Applicant notification email added"))),
                div().withClass("flex-grow"),
                div()
                    .condWith(
                        status.defaultStatus().isPresent()
                            && status.defaultStatus().get().equals(true),
                        p().withClasses("mt-1", "text-xs", "flex", "items-center")
                            .with(
                                Icons.svg(Icons.CHECK)
                                    // 4.5 is 18px as defined in tailwind.config.js
                                    .withClasses("mr-2", "inline-block", "h-4.5", "w-4.5"),
                                span("Default status"))),
                div().withClass("flex-grow"),
                deleteStatusTriggerButton,
                editStatusTriggerButton),
        ImmutableList.of(editStatusModal, deleteStatusModal));
  }

  private Modal makeStatusDeleteModal(
      Http.Request request, ProgramDefinition program, String toDeleteStatusText) {
    DivTag content =
        div()
            .withClasses("py-2")
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
                            .withClasses("flex", "mt-5", "space-x-2")
                            .with(
                                div().withClass("flex-grow"),
                                submitButton("Delete")
                                    .withClass(ButtonStyles.OUTLINED_WHITE_WITH_ICON))));

    return Modal.builder()
        .setModalId(Modal.randomModalId())
        .setLocation(Modal.Location.ADMIN_FACING)
        .setContent(content)
        .setModalTitle("Delete this status")
        .build();
  }

  /**
   * Returns whether the form is for creating a new status (signified by the "configuredStatusText"
   * field being empty).
   */
  private static boolean isCreationForm(Optional<Form<ProgramStatusesForm>> maybeForm) {
    return maybeForm.map(f -> f.value().get().getConfiguredStatusText().isEmpty()).orElse(false);
  }

  /**
   * Returns whether the form matches the status object being rendered. The page has multiple inline
   * modals for editing and creating statuses.
   */
  private static boolean isFormForStatus(
      Optional<Form<ProgramStatusesForm>> maybeEditForm, StatusDefinitions.Status status) {
    return maybeEditForm.isPresent()
        && status
            .statusText()
            .equalsIgnoreCase(maybeEditForm.get().value().get().getConfiguredStatusText());
  }

  private Modal makeStatusUpdateModal(
      Http.Request request,
      ProgramDefinition program,
      Form<ProgramStatusesForm> form,
      boolean displayOnLoad,
      boolean showEmailDeletionWarning) {
    Messages messages = messagesApi.preferred(request);
    ProgramStatusesForm formData = form.value().get();

    Boolean isCurrentDefault = formData.getDefaultStatus().orElse(false);
    LabelTag defaultCheckbox =
        label()
            .with(
                input()
                    .withType("checkbox")
                    .withName(ProgramStatusesForm.DEFAULT_CHECKBOX_NAME)
                    .withCondChecked(isCurrentDefault)
                    .withClasses(BaseStyles.CHECKBOX, "cf-set-default-status-checkbox"),
                span("Set as default status"));
    Optional<StatusDefinitions.Status> currentDefaultStatus =
        program.toProgram().getDefaultStatus();
    String messagePart =
        currentDefaultStatus
            .map(status -> String.format("from %s to ", status.statusText()))
            .orElse("to ");
    FormTag content =
        form()
            .withMethod("POST")
            .withAction(routes.AdminProgramStatusesController.createOrUpdate(program.id()).url())
            .withClasses("cf-status-change-form", "py-2")
            .withData("dontshow", isCurrentDefault.toString())
            .withData("messagepart", messagePart)
            .with(
                makeCsrfTokenInputTag(request),
                input()
                    .isHidden()
                    .withName(ProgramStatusesForm.CONFIGURED_STATUS_TEXT_FORM_NAME)
                    .withValue(formData.getConfiguredStatusText()),
                renderFormGlobalErrors(messages, form),
                FieldWithLabel.input()
                    .setFieldName(ProgramStatusesForm.STATUS_TEXT_FORM_NAME)
                    .setLabelText("Status name (required)")
                    .setValue(formData.getStatusText())
                    .setFieldErrors(
                        messages, form.errors(ProgramStatusesForm.STATUS_TEXT_FORM_NAME))
                    .getInputTag(),
                div()
                    .withClasses("pt-8")
                    .with(
                        FieldWithLabel.textArea()
                            .setFieldName(ProgramStatusesForm.EMAIL_BODY_FORM_NAME)
                            .setLabelText("Email the applicant about the status change")
                            .setRows(OptionalLong.of(5))
                            .setValue(formData.getEmailBody())
                            .setFieldErrors(
                                messages, form.errors(ProgramStatusesForm.EMAIL_BODY_FORM_NAME))
                            .getTextareaTag()))
            .condWith(showEmailDeletionWarning, renderEmailTranslationWarning())
            .with(
                div()
                    .withClasses("flex", "mt-5", "space-x-2")
                    .with(
                        defaultCheckbox,
                        div().withClass("flex-grow"),
                        submitButton("Confirm").withClass(ButtonStyles.CLEAR_WITH_ICON)));
    return Modal.builder()
        .setModalId(Modal.randomModalId())
        .setLocation(Modal.Location.ADMIN_FACING)
        .setContent(content)
        .setModalTitle(
            formData.getConfiguredStatusText().isEmpty()
                ? "Create a new status"
                : "Edit this status")
        .setWidth(Width.HALF)
        .setDisplayOnLoad(displayOnLoad)
        .build();
  }

  private PTag renderEmailTranslationWarning() {
    return p("Please be aware that clearing the email body will also clear any associated"
            + " translations")
        .withClasses(
            "m-2", "p-2", "text-sm", "border", "rounded-lg", "border-amber-400", "bg-amber-200");
  }

  private DivTag renderFormGlobalErrors(Messages messages, Form<ProgramStatusesForm> form) {
    ImmutableList<String> errors =
        form.globalErrors().stream()
            .map(e -> e.format(messages))
            .collect(ImmutableList.toImmutableList());
    return errors.isEmpty()
        ? div()
        : div(each(errors, e -> p(e).withClasses("text-sm", "text-red-600"))).withClasses("pb-4");
  }
}
