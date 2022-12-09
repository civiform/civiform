package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.option;
import static j2html.TagCreator.p;
import static j2html.TagCreator.select;
import static j2html.TagCreator.span;

import annotations.BindingAnnotations.EnUsLang;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;
import featureflags.FeatureFlags;
import j2html.tags.DomContent;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.SelectTag;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalLong;
import models.Application;
import org.apache.commons.lang3.RandomStringUtils;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.AnswerData;
import services.applicant.Block;
import services.program.StatusDefinitions;
import views.BaseHtmlLayout;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.Modal.Width;
import views.components.ToastMessage;
import views.style.AdminStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Renders a page for a program admin to view a single submitted application. */
public final class ProgramApplicationView extends BaseHtmlView {

  private static final String PROGRAM_ID = "programId";
  private static final String APPLICATION_ID = "applicationId";
  public static final String SEND_EMAIL = "sendEmail";
  public static final String CURRENT_STATUS = "currentStatus";
  public static final String NEW_STATUS = "newStatus";
  public static final String NOTE = "note";
  private final BaseHtmlLayout layout;
  private final Messages enUsMessages;
  private final FeatureFlags featureFlags;

  @Inject
  public ProgramApplicationView(
      BaseHtmlLayout layout, @EnUsLang Messages enUsMessages, FeatureFlags featureFlags) {
    this.layout = checkNotNull(layout);
    this.enUsMessages = checkNotNull(enUsMessages);
    this.featureFlags = checkNotNull(featureFlags);
  }

  public Content render(
      long programId,
      String programName,
      Application application,
      String applicantNameWithApplicationId,
      ImmutableList<Block> blocks,
      ImmutableList<AnswerData> answers,
      StatusDefinitions statusDefinitions,
      Optional<String> noteMaybe,
      Http.Request request) {
    String title = "Program Application View";
    ListMultimap<Block, AnswerData> blockToAnswers = ArrayListMultimap.create();
    for (AnswerData answer : answers) {
      Block answerBlock =
          blocks.stream()
              .filter(block -> block.getId().equals(answer.blockId()))
              .findFirst()
              .orElseThrow();
      blockToAnswers.put(answerBlock, answer);
    }

    ImmutableList<Modal> statusUpdateConfirmationModals =
        statusDefinitions.getStatuses().stream()
            .map(
                status ->
                    renderStatusUpdateConfirmationModal(
                        programId,
                        programName,
                        application,
                        applicantNameWithApplicationId,
                        status))
            .collect(ImmutableList.toImmutableList());
    Modal updateNoteModal = renderUpdateNoteConfirmationModal(programId, application, noteMaybe);

    DivTag contentDiv =
        div()
            .withId("application-view")
            .withClasses("px-20")
            .with(
                h2("Program: " + programName).withClasses("my-4"),
                div()
                    .withClasses("flex")
                    .with(
                        p(applicantNameWithApplicationId)
                            .withClasses(
                                "my-4",
                                "text-black",
                                "text-2xl",
                                "mb-2",
                                ReferenceClasses.BT_APPLICATION_ID),
                        // Spread out the items, so the following are right
                        // aligned.
                        p().withClasses("flex-grow"))
                    // Status options if configured on the program.
                    .condWith(
                        !statusDefinitions.getStatuses().isEmpty(),
                        div()
                            .withClasses("flex", "mr-4", "space-x-2")
                            .with(
                                renderStatusOptionsSelector(application, statusDefinitions),
                                updateNoteModal.getButton()))
                    .with(renderDownloadButton(programId, application.id)))
            .with(
                each(
                    blocks,
                    block -> renderApplicationBlock(programId, block, blockToAnswers.get(block))))
            .with(each(statusUpdateConfirmationModals, Modal::getButton));

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainContent(contentDiv)
            // The body and main styles are necessary for modals to appear since they use fixed
            // sizing.
            .addBodyStyles("flex")
            .addMainStyles("w-screen")
            .addModals(updateNoteModal)
            .addModals(statusUpdateConfirmationModals);
    if (!featureFlags.isJsBundlingEnabled()) {
      htmlBundle.addFooterScripts(layout.viewUtils.makeLocalJsTag("admin_application_view"));
    }
    Optional<String> maybeSuccessMessage = request.flash().get("success");
    if (maybeSuccessMessage.isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.success(maybeSuccessMessage.get()));
    }
    return layout.render(htmlBundle);
  }

  private ATag renderDownloadButton(long programId, long applicationId) {
    String link =
        controllers.admin.routes.AdminApplicationController.download(programId, applicationId)
            .url();
    return new LinkElement().setHref(link).setText("Export to PDF").asRightAlignedButton();
  }

  private DivTag renderApplicationBlock(
      long programId, Block block, Collection<AnswerData> answers) {
    DivTag topContent =
        div()
            .withClasses("flex")
            .with(
                div(div(block.getName()).withClasses("text-black", "font-bold", "text-xl", "mb-2")))
            .with(p().withClasses("flex-grow"))
            .with(p(block.getDescription()).withClasses("text-gray-700", "italic"));

    DivTag mainContent =
        div().withClasses("w-full").with(each(answers, answer -> renderAnswer(programId, answer)));

    DivTag innerDiv =
        div(topContent, mainContent)
            .withClasses("border", "border-gray-300", "bg-white", "rounded", "p-4");

    return div(innerDiv)
        .withClasses(ReferenceClasses.ADMIN_APPLICATION_BLOCK_CARD, "w-full", "shadow-lg", "mb-4");
  }

  private DivTag renderAnswer(long programId, AnswerData answerData) {
    LocalDate date =
        Instant.ofEpochMilli(answerData.timestamp()).atZone(ZoneId.systemDefault()).toLocalDate();
    DivTag answerContent;
    if (answerData.encodedFileKey().isPresent()) {
      String encodedFileKey = answerData.encodedFileKey().get();
      String fileLink =
          controllers.routes.FileController.adminShow(programId, encodedFileKey).url();
      answerContent = div(a(answerData.answerText()).withHref(fileLink));
    } else {
      answerContent = div(answerData.answerText());
    }
    return div()
        .withClasses("flex")
        .with(
            div()
                .withClasses("mb-8")
                .with(
                    div(answerData.questionDefinition().getName())
                        .withClasses("text-gray-400", "text-base", "line-clamp-3")))
        .with(p().withClasses("w-8"))
        .with(answerContent.withClasses("text-gray-700", "text-base", "line-clamp-3"))
        .with(p().withClasses("flex-grow"))
        .with(
            div("Answered on " + date)
                .withClasses("flex-auto", "text-right", "font-light", "text-xs"));
  }

  private DivTag renderStatusOptionsSelector(
      Application application, StatusDefinitions statusDefinitions) {
    final String SELECTOR_ID = RandomStringUtils.randomAlphabetic(8);
    DivTag container =
        div()
            .withClasses("flex", ReferenceClasses.PROGRAM_ADMIN_STATUS_SELECTOR)
            .with(label("Status:").withClasses("self-center").withFor(SELECTOR_ID));

    SelectTag dropdownTag =
        select()
            .withId(SELECTOR_ID)
            .withClasses(
                "outline-none",
                "px-3",
                "py-1",
                "ml-3",
                "my-4",
                "border",
                "border-gray-500",
                "rounded-full",
                "bg-white",
                "text-xs",
                StyleUtils.focus(BaseStyles.BORDER_SEATTLE_BLUE));

    // Add the options available to the admin.
    // When no status is currently applied to the application, add a placeholder option that is
    // selected.
    dropdownTag.with(
        option(enUsMessages.at(MessageKey.DROPDOWN_PLACEHOLDER.getKeyName()))
            .isDisabled()
            .withCondSelected(application.getLatestStatus().isEmpty()));

    // Add statuses in the order they're provided.
    String latestStatusText = application.getLatestStatus().orElse("");
    statusDefinitions.getStatuses().stream()
        .map(StatusDefinitions.Status::statusText)
        .forEach(
            statusText -> {
              boolean isCurrentStatus = statusText.equals(latestStatusText);
              dropdownTag.with(
                  option(statusText).withValue(statusText).withCondSelected(isCurrentStatus));
            });
    return container.with(dropdownTag);
  }

  private Modal renderUpdateNoteConfirmationModal(
      long programId, Application application, Optional<String> noteMaybe) {
    ButtonTag triggerButton =
        makeSvgTextButton("Edit note", Icons.EDIT).withClasses(AdminStyles.TERTIARY_BUTTON_STYLES);
    String formId = Modal.randomModalId();
    // No form action or content is rendered since admin_application_view.ts extracts the values
    // and calls postMessage rather than attempting a submission. The main frame is responsible for
    // constructing a form to update the note.
    FormTag modalContent =
        form().withId(formId).withClasses("px-6", "py-2", "cf-program-admin-edit-note-form");
    modalContent.with(
        input().withName(PROGRAM_ID).withValue(Long.toString(programId)).isHidden(),
        input().withName(APPLICATION_ID).withValue(Long.toString(application.id)).isHidden(),
        FieldWithLabel.textArea()
            .setValue(noteMaybe)
            .setFormId(formId)
            .setFieldName(NOTE)
            .setRows(OptionalLong.of(8))
            .getTextareaTag(),
        div()
            .withClasses("flex", "mt-5", "space-x-2")
            .with(
                div().withClass("flex-grow"),
                button("Cancel")
                    .withClasses(ReferenceClasses.MODAL_CLOSE, AdminStyles.TERTIARY_BUTTON_STYLES),
                submitButton("Save").withClass(AdminStyles.TERTIARY_BUTTON_STYLES)));
    return Modal.builder(Modal.randomModalId(), modalContent)
        .setModalTitle("Edit note")
        .setTriggerButtonContent(triggerButton)
        .setWidth(Width.THREE_FOURTHS)
        .build();
  }

  private Modal renderStatusUpdateConfirmationModal(
      long programId,
      String programName,
      Application application,
      String applicantNameWithApplicationId,
      StatusDefinitions.Status status) {
    // The previous status as it should be displayed and passed as data in the
    // update.
    String previousStatusDisplay = application.getLatestStatus().orElse("Unset");
    String previousStatusData = application.getLatestStatus().orElse("");
    // No form action or content is rendered since admin_application_view.ts extracts the values
    // and calls postMessage rather than attempting a submission. The main frame is responsible for
    // constructing a form to update the status.
    FormTag modalContent =
        form()
            .withClasses("px-6", "py-2", "cf-program-admin-status-update-form")
            .with(
                input().withName(PROGRAM_ID).withValue(Long.toString(programId)).isHidden(),
                input()
                    .withName(APPLICATION_ID)
                    .withValue(Long.toString(application.id))
                    .isHidden(),
                p().with(
                        span("Status Change: "),
                        span(previousStatusDisplay).withClass("font-semibold"),
                        span(" -> ").withClass("font-semibold"),
                        span(status.statusText()).withClass("font-semibold"),
                        span(" (visible to applicant)")),
                p().with(
                        span("Applicant: "),
                        span(applicantNameWithApplicationId)
                            .withClasses("font-semibold", ReferenceClasses.BT_APPLICATION_ID)),
                p().with(span("Program: "), span(programName).withClass("font-semibold")),
                div()
                    .withClasses("mt-4")
                    // Add the new status to the form hidden.
                    .with(
                        input()
                            .isHidden()
                            .withType("text")
                            .withName(NEW_STATUS)
                            .withValue(status.statusText()))
                    // Add the original status to the form hidden so that we can
                    // detect if the data has changed since this UI was
                    // generated.
                    .with(
                        input()
                            .isHidden()
                            .withType("text")
                            .withName(CURRENT_STATUS)
                            .withValue(previousStatusData))
                    .with(
                        renderStatusUpdateConfirmationModalEmailSection(
                            applicantNameWithApplicationId, application, status)),
                div()
                    .withClasses("flex", "mt-5", "space-x-2")
                    .with(
                        div().withClass("flex-grow"),
                        button("Cancel")
                            .withClasses(
                                ReferenceClasses.MODAL_CLOSE, AdminStyles.TERTIARY_BUTTON_STYLES),
                        submitButton("Confirm").withClass(AdminStyles.TERTIARY_BUTTON_STYLES)));
    ButtonTag triggerButton =
        button("")
            .withClasses("hidden")
            .withData("status-update-confirm-for-status", status.statusText());
    return Modal.builder(Modal.randomModalId(), modalContent)
        .setModalTitle("Change the status of this application?")
        .setWidth(Width.THREE_FOURTHS)
        .setTriggerButtonContent(triggerButton)
        .build();
  }

  private DomContent renderStatusUpdateConfirmationModalEmailSection(
      String applicantNameWithApplicationId,
      Application application,
      StatusDefinitions.Status status) {
    InputTag sendEmailInput =
        input().withType("checkbox").withName(SEND_EMAIL).withClasses(BaseStyles.CHECKBOX);
    Optional<String> maybeApplicantEmail =
        Optional.ofNullable(application.getApplicant().getAccount().getEmailAddress());
    if (status.localizedEmailBodyText().isEmpty()) {
      return div()
          .with(
              sendEmailInput.isHidden(),
              p().with(
                      span(applicantNameWithApplicationId)
                          .withClasses("font-semibold", ReferenceClasses.BT_APPLICATION_ID),
                      span(
                          " will not receive an email because there is no email content set for"
                              + " this status. Connect with your CiviForm Admin to add an email to"
                              + " this status.")));
    } else if (maybeApplicantEmail.isEmpty()) {
      return div()
          .with(
              sendEmailInput.isHidden(),
              p().with(
                      span(applicantNameWithApplicationId).withClass("font-semibold"),
                      span(
                          " will not receive an email for this change since they have not provided"
                              + " an email address.")));
    }
    return label()
        .with(
            // Check by default when visible.
            sendEmailInput.isChecked(),
            span("Notify "),
            span(applicantNameWithApplicationId).withClass("font-semibold"),
            span(" of this change at "),
            span(maybeApplicantEmail.orElse("")).withClass("font-semibold"));
  }
}
