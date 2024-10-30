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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;
import controllers.FlashKey;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.SelectTag;
import j2html.tags.specialized.SpanTag;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalLong;
import models.ApplicationModel;
import org.apache.commons.lang3.RandomStringUtils;
import play.i18n.Messages;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.DateConverter;
import services.MessageKey;
import services.applicant.AnswerData;
import services.applicant.Block;
import services.settings.SettingsManifest;
import services.statuses.StatusDefinitions;
import views.BaseHtmlLayout;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.JsBundle;
import views.ViewUtils;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.Modal.Width;
import views.components.ToastMessage;
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
  private final DateConverter dateConverter;
  private final SettingsManifest settingsManifest;

  @Inject
  public ProgramApplicationView(
      BaseHtmlLayout layout,
      @EnUsLang Messages enUsMessages,
      DateConverter dateConverter,
      SettingsManifest settingsManifest) {
    this.layout = checkNotNull(layout);
    this.enUsMessages = checkNotNull(enUsMessages);
    this.dateConverter = checkNotNull(dateConverter);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  public Content render(
      long programId,
      String programName,
      ApplicationModel application,
      String applicantNameWithApplicationId,
      ImmutableList<Block> blocks,
      ImmutableList<AnswerData> answers,
      StatusDefinitions statusDefinitions,
      Optional<String> noteMaybe,
      Boolean hasEligibilityEnabled,
      Request request) {
    String title = "Program application view";
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
                settingsManifest.getBulkStatusUpdateEnabled(request)
                    ? renderBackLink(programId)
                    : div(),
                div()
                    .withClasses(
                        "flex", "flex-wrap", "items-center", "my-4", "gap-2", "justify-between")
                    .with(
                        p(applicantNameWithApplicationId)
                            .withClasses(
                                "text-black", "text-2xl", ReferenceClasses.BT_APPLICATION_ID))
                    .with(
                        div()
                            .withClasses("flex", "flex-wrap", "gap-2")
                            // Status options if configured on the program.
                            .condWith(
                                !statusDefinitions.getStatuses().isEmpty(),
                                div()
                                    .withClasses("flex", "mr-4", "gap-2")
                                    .with(
                                        renderStatusOptionsSelector(application, statusDefinitions),
                                        updateNoteModal.getButton()))
                            .with(renderDownloadButton(programId, application.id))))
            .with(
                p(renderSubmitTime(application))
                    .withClasses("text-xs", "text-gray-700", "mb-2", ReferenceClasses.BT_DATE))
            .with(
                each(
                    blocks,
                    block ->
                        renderApplicationBlock(
                            block, blockToAnswers.get(block), hasEligibilityEnabled)))
            .with(each(statusUpdateConfirmationModals, Modal::getButton));

    HtmlBundle htmlBundle =
        layout
            .getBundle(request)
            .setTitle(title)
            .addMainContent(contentDiv)
            // The body and main styles are necessary for modals to appear since they use fixed
            // sizing.
            .addBodyStyles("flex")
            .addMainStyles("w-screen")
            .addModals(updateNoteModal)
            .addModals(statusUpdateConfirmationModals)
            .setJsBundle(JsBundle.ADMIN);
    Optional<String> maybeSuccessMessage = request.flash().get(FlashKey.SUCCESS);
    if (maybeSuccessMessage.isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.success(maybeSuccessMessage.get()));
    }
    return layout.render(htmlBundle);
  }

  private ATag renderBackLink(Long programId) {
    String backUrl =
        routes.AdminApplicationController.index(
                programId,
                /* search= */ Optional.empty(),
                /* page= */ Optional.empty(),
                /* fromDate= */ Optional.empty(),
                /* untilDate= */ Optional.empty(),
                /* applicationStatus= */ Optional.empty(),
                /* selectedApplicationUri= */ Optional.empty(),
                /* showDownloadModal= */ Optional.empty(),
                /* message= */ Optional.empty())
            .url();

    return new LinkElement()
        .setId("application-table-view-")
        .setHref(backUrl)
        .setText("Back")
        .setStyles(
            "mr-2", ReferenceClasses.VIEW_BUTTON, "underline", ReferenceClasses.BT_APPLICATION_ID)
        .asAnchorText();
  }

  private ButtonTag renderDownloadButton(long programId, long applicationId) {
    String link =
        controllers.admin.routes.AdminApplicationController.download(programId, applicationId)
            .url();
    return asRedirectElement(
        ViewUtils.makeSvgTextButton("Export to PDF", Icons.DOWNLOAD)
            .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON),
        link);
  }

  private DivTag renderApplicationBlock(
      Block block, Collection<AnswerData> answers, boolean hasEligibilityEnabled) {
    DivTag topContent =
        div()
            .withClasses("flex")
            .with(
                div(div(block.getName()).withClasses("text-black", "font-bold", "text-xl", "mb-2")))
            .with(p().withClasses("flex-grow"))
            .with(p(block.getDescription()).withClasses("text-gray-700", "italic"));

    boolean isEligibilityEnabledInBlock =
        hasEligibilityEnabled && block.getEligibilityDefinition().isPresent();
    DivTag mainContent =
        div()
            .withClasses("w-full")
            .with(
                each(
                    answers,
                    answer ->
                        renderAnswer(
                            answer,
                            // If an eligibility predicate is defined for the block, check if
                            // the question is part of the predicate to determine whether to
                            // show the eligibility status.
                            isEligibilityEnabledInBlock
                                && block
                                    .getEligibilityDefinition()
                                    .map(
                                        definition ->
                                            definition
                                                .predicate()
                                                .getQuestions()
                                                .contains(answer.questionDefinition().getId()))
                                    .orElse(false))));

    DivTag innerDiv =
        div(topContent, mainContent)
            .withClasses("border", "border-gray-300", "bg-white", "rounded", "p-4");

    return div(innerDiv)
        .withClasses(ReferenceClasses.ADMIN_APPLICATION_BLOCK_CARD, "w-full", "shadow-lg", "mb-4");
  }

  private DivTag renderAnswer(AnswerData answerData, boolean showEligibilityText) {
    String date = dateConverter.renderDate(Instant.ofEpochMilli(answerData.timestamp()));
    DivTag answerContent;
    if (!answerData.encodedFileKeys().isEmpty()) {
      answerContent = div();
      for (int i = 0; i < answerData.encodedFileKeys().size(); i++) {
        String encodedFileKey = answerData.encodedFileKeys().get(i);
        String fileName = answerData.fileNames().get(i);
        String fileLink = controllers.routes.FileController.acledAdminShow(encodedFileKey).url();
        answerContent.with(a(fileName).withHref(fileLink).withClass(BaseStyles.LINK_TEXT));
        if (i < answerData.encodedFileKeys().size() - 1) {
          answerContent.with(span(", "));
        }
      }
    } else if (answerData.encodedFileKey().isPresent()) {
      String encodedFileKey = answerData.encodedFileKey().get();
      String fileLink = controllers.routes.FileController.acledAdminShow(encodedFileKey).url();
      answerContent = div(a(answerData.answerText()).withHref(fileLink));
    } else {
      answerContent = div(answerData.answerText().replace("\n", "; "));
    }
    DivTag eligibilityAndTimestampDiv =
        div().withClasses("flex-auto", "text-right", "font-light", "text-xs");
    eligibilityAndTimestampDiv.with(
        div("Answered on " + date).withClasses(ReferenceClasses.BT_DATE));
    if (showEligibilityText) {
      String eligibilityText =
          answerData.isEligible() ? "Meets eligibility" : "Doesn't meet eligibility";
      eligibilityAndTimestampDiv.with(div(eligibilityText));
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
        .with(eligibilityAndTimestampDiv);
  }

  private DivTag renderStatusOptionsSelector(
      ApplicationModel application, StatusDefinitions statusDefinitions) {
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
                "py-2",
                "ml-3",
                "border",
                "border-gray-500",
                "rounded-lg",
                "bg-white",
                "text-lg",
                StyleUtils.focus(BaseStyles.BORDER_CIVIFORM_BLUE));

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
      long programId, ApplicationModel application, Optional<String> noteMaybe) {
    ButtonTag triggerButton =
        makeSvgTextButton("Edit note", Icons.EDIT).withClasses(ButtonStyles.CLEAR_WITH_ICON);
    String formId = Modal.randomModalId();
    // No form action or content is rendered since admin_application_view.ts extracts the values
    // and calls postMessage rather than attempting a submission. The main frame is responsible for
    // constructing a form to update the note.
    FormTag modalContent = form().withId(formId).withClasses("cf-program-admin-edit-note-form");
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
                    .withClasses(ReferenceClasses.MODAL_CLOSE, ButtonStyles.CLEAR_WITH_ICON),
                submitButton("Save").withClass(ButtonStyles.CLEAR_WITH_ICON)));
    return Modal.builder()
        .setModalId(Modal.randomModalId())
        .setLocation(Modal.Location.ADMIN_FACING)
        .setContent(modalContent)
        .setModalTitle("Edit note")
        .setTriggerButtonContent(triggerButton)
        .setWidth(Width.THREE_FOURTHS)
        .setTop(Modal.Top.QUARTER)
        .build();
  }

  private Modal renderStatusUpdateConfirmationModal(
      long programId,
      String programName,
      ApplicationModel application,
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
            .withClasses("cf-program-admin-status-update-form")
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
                                ReferenceClasses.MODAL_CLOSE, ButtonStyles.CLEAR_WITH_ICON),
                        submitButton("Confirm").withClass(ButtonStyles.CLEAR_WITH_ICON)));
    ButtonTag triggerButton =
        button("")
            .withClasses("hidden")
            .withData("status-update-confirm-for-status", status.statusText());
    return Modal.builder()
        .setModalId(Modal.randomModalId())
        .setLocation(Modal.Location.ADMIN_FACING)
        .setContent(modalContent)
        .setModalTitle("Change the status of this application?")
        .setWidth(Width.THREE_FOURTHS)
        .setTriggerButtonContent(triggerButton)
        .setTop(Modal.Top.QUARTER)
        .build();
  }

  private DomContent renderStatusUpdateConfirmationModalEmailSection(
      String applicantNameWithApplicationId,
      ApplicationModel application,
      StatusDefinitions.Status status) {
    InputTag sendEmailInput =
        input().withType("checkbox").withName(SEND_EMAIL).withClasses(BaseStyles.CHECKBOX);

    Optional<String> optionalAccountEmail =
        Optional.ofNullable(application.getApplicant().getAccount().getEmailAddress());
    Optional<String> optionalApplicantEmail = application.getApplicant().getEmailAddress();
    boolean emptyEmails = optionalAccountEmail.isEmpty();

    if (settingsManifest.getPrimaryApplicantInfoQuestionsEnabled()) {
      emptyEmails = emptyEmails && optionalApplicantEmail.isEmpty();
    }

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
    } else if (emptyEmails) {
      return div()
          .with(
              sendEmailInput.isHidden(),
              p().with(
                      span(applicantNameWithApplicationId).withClass("font-semibold"),
                      span(
                          " will not receive an email for this change since they have not provided"
                              + " an email address.")));
    }

    String emailString = "";
    if (settingsManifest.getPrimaryApplicantInfoQuestionsEnabled()) {
      emailString = generateEmailString(optionalAccountEmail, optionalApplicantEmail);
    } else {
      emailString = optionalAccountEmail.orElse("");
    }

    return label()
        .with(
            // Check by default when visible.
            sendEmailInput.isChecked(),
            span("Notify "),
            span(applicantNameWithApplicationId)
                .withClasses("font-semibold", ReferenceClasses.BT_APPLICATION_ID),
            span(" of this change at "),
            span(emailString).withClass("font-semibold"));
  }

  private String generateEmailString(
      Optional<String> optionalAccountEmail, Optional<String> optionalApplicantEmail) {

    // Create a set to handle the case where both emails are the same.
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    optionalAccountEmail.ifPresent(builder::add);
    optionalApplicantEmail.ifPresent(builder::add);
    // Join the emails with " and " if there are two, otherwise just return the single email.
    return String.join(" and ", builder.build());
  }

  private SpanTag renderSubmitTime(ApplicationModel application) {
    String submitTime =
        application.getSubmitTime() == null
            ? "Application submitted without submission time marked."
            : dateConverter.renderDateTimeHumanReadable(application.getSubmitTime());
    return span().withText(submitTime);
  }
}
