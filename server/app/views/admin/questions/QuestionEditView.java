package views.admin.questions;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.input;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import forms.QuestionForm;
import forms.QuestionFormBuilder;
import j2html.tags.DomContent;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FieldsetTag;
import j2html.tags.specialized.FormTag;
import java.util.Locale;
import java.util.Optional;
import models.QuestionTag;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.export.CsvExporterService;
import services.question.PrimaryApplicantInfoTag;
import services.question.QuestionService;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.settings.SettingsManifest;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.ViewUtils;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.applicant.ApplicantFileUploadRenderer;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.SelectWithLabel;
import views.components.ToastMessage;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Renders a page for editing a question. */
public final class QuestionEditView extends BaseHtmlView {
  private final AdminLayout layout;
  private final Messages messages;
  private final ApplicantFileUploadRenderer applicantFileUploadRenderer;
  private final QuestionService questionService;
  private final SettingsManifest settingsManifest;

  private static final String NO_ENUMERATOR_DISPLAY_STRING = "does not repeat";
  private static final String NO_ENUMERATOR_ID_STRING = "";
  private static final String QUESTION_NAME_FIELD = "questionName";
  private static final String QUESTION_ENUMERATOR_FIELD = "enumeratorId";

  @Inject
  public QuestionEditView(
      AdminLayoutFactory layoutFactory,
      MessagesApi messagesApi,
      ApplicantFileUploadRenderer applicantFileUploadRenderer,
      QuestionService questionService,
      SettingsManifest settingsManifest) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.QUESTIONS);
    // Use the default language for CiviForm, since this is an admin view and not applicant-facing.
    this.messages = messagesApi.preferred(ImmutableList.of(Lang.defaultLang()));
    this.applicantFileUploadRenderer = checkNotNull(applicantFileUploadRenderer);
    this.questionService = checkNotNull(questionService);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  /** Render a fresh New Question Form. */
  public Content renderNewQuestionForm(
      Request request,
      QuestionType questionType,
      ImmutableList<EnumeratorQuestionDefinition> enumeratorQuestionDefinitions,
      String redirectUrl)
      throws UnsupportedQuestionTypeException {
    QuestionForm questionForm = QuestionFormBuilder.create(questionType);
    questionForm.setRedirectUrl(redirectUrl);
    return renderNewQuestionForm(
        request, questionForm, enumeratorQuestionDefinitions, /* message= */ Optional.empty());
  }

  /** Render a New Question Form with a partially filled form and an error message. */
  public Content renderNewQuestionForm(
      Request request,
      QuestionForm questionForm,
      ImmutableList<EnumeratorQuestionDefinition> enumeratorQuestionDefinitions,
      ToastMessage errorMessage) {
    return renderNewQuestionForm(
        request, questionForm, enumeratorQuestionDefinitions, Optional.of(errorMessage));
  }

  private Content renderNewQuestionForm(
      Request request,
      QuestionForm questionForm,
      ImmutableList<EnumeratorQuestionDefinition> enumeratorQuestionDefinitions,
      Optional<ToastMessage> message) {
    QuestionType questionType = questionForm.getQuestionType();
    String title =
        String.format("New %s question", questionType.getLabel().toLowerCase(Locale.ROOT));

    DivTag formContent =
        buildQuestionContainer(title)
            .with(
                buildNewQuestionForm(questionForm, enumeratorQuestionDefinitions, request)
                    .with(makeCsrfTokenInputTag(request)));

    message
        .map(m -> m.setDismissible(true))
        .map(ToastMessage::getContainerTag)
        .ifPresent(formContent::with);

    return renderWithPreview(
        request, formContent, questionType, title, /* modal= */ Optional.empty());
  }

  /** Render a fresh Edit Question Form. */
  public Content renderEditQuestionForm(
      Request request,
      QuestionDefinition questionDefinition,
      Optional<QuestionDefinition> maybeEnumerationQuestionDefinition)
      throws InvalidQuestionTypeException {
    QuestionForm questionForm = QuestionFormBuilder.create(questionDefinition);
    return renderEditQuestionForm(
        request,
        questionDefinition.getId(),
        questionForm,
        maybeEnumerationQuestionDefinition,
        Optional.empty());
  }

  /** Render a Edit Question form with errors. */
  public Content renderEditQuestionForm(
      Request request,
      long id,
      QuestionForm questionForm,
      Optional<QuestionDefinition> maybeEnumerationQuestionDefinition,
      ToastMessage message) {
    return renderEditQuestionForm(
        request, id, questionForm, maybeEnumerationQuestionDefinition, Optional.of(message));
  }

  private Content renderEditQuestionForm(
      Request request,
      long id,
      QuestionForm questionForm,
      Optional<QuestionDefinition> maybeEnumerationQuestionDefinition,
      Optional<ToastMessage> message) {

    Modal unsetUniversalModal = buildUnsetUniversalModal(questionForm);

    QuestionType questionType = questionForm.getQuestionType();
    String title =
        String.format("Edit %s question", questionType.getLabel().toLowerCase(Locale.ROOT));

    // When removing the UNIVERSAL_QUESTIONS feature flag, remove passing through
    // the request down to buildQuestionForm.
    DivTag formContent =
        buildQuestionContainer(title)
            .with(
                buildEditQuestionForm(
                        id,
                        questionForm,
                        maybeEnumerationQuestionDefinition,
                        request,
                        unsetUniversalModal)
                    .with(makeCsrfTokenInputTag(request)));

    message
        .map(m -> m.setDismissible(true))
        .map(ToastMessage::getContainerTag)
        .ifPresent(formContent::with);

    return renderWithPreview(
        request, formContent, questionType, title, Optional.of(unsetUniversalModal));
  }

  /** Render a read-only non-submittable question form. */
  public Content renderViewQuestionForm(
      Request request,
      QuestionDefinition questionDefinition,
      Optional<QuestionDefinition> maybeEnumerationQuestionDefinition)
      throws InvalidQuestionTypeException {
    QuestionForm questionForm = QuestionFormBuilder.create(questionDefinition);
    QuestionType questionType = questionForm.getQuestionType();
    String title =
        String.format("View %s question", questionType.toString().toLowerCase(Locale.ROOT));

    SelectWithLabel enumeratorOption =
        enumeratorOptionsFromMaybeEnumerationQuestionDefinition(maybeEnumerationQuestionDefinition);
    DivTag formContent =
        buildQuestionContainer(title)
            .with(buildReadOnlyQuestionForm(questionForm, enumeratorOption, request));

    return renderWithPreview(
        request, formContent, questionType, title, /* modal= */ Optional.empty());
  }

  private Content renderWithPreview(
      Request request, DivTag formContent, QuestionType type, String title, Optional<Modal> modal) {
    DivTag previewContent =
        QuestionPreview.renderQuestionPreview(type, messages, applicantFileUploadRenderer);

    HtmlBundle htmlBundle =
        layout.getBundle(request).setTitle(title).addMainContent(formContent, previewContent);

    if (settingsManifest.getUniversalQuestions(request) && modal.isPresent()) {
      htmlBundle.addModals(modal.get());
    }
    return layout.render(htmlBundle);
  }

  private FormTag buildSubmittableQuestionForm(
      QuestionForm questionForm,
      SelectWithLabel enumeratorOptions,
      boolean forCreate,
      Request request) {
    return buildQuestionForm(
        questionForm, enumeratorOptions, /* submittable= */ true, forCreate, request);
  }

  private FormTag buildReadOnlyQuestionForm(
      QuestionForm questionForm, SelectWithLabel enumeratorOptions, Request request) {
    return buildQuestionForm(
        questionForm, enumeratorOptions, /* submittable= */ false, /* forCreate= */ false, request);
  }

  private DivTag buildQuestionContainer(String title) {
    return div()
        .withId("question-form")
        .withClasses(
            "border-gray-400",
            "border-r",
            "p-6",
            "flex",
            "flex-col",
            "h-full",
            "overflow-hidden",
            "overflow-y-auto",
            "relative",
            "w-2/5")
        .with(renderHeader(title))
        .with(multiOptionQuestionField());
  }

  // A hidden template for multi-option questions.
  private DivTag multiOptionQuestionField() {
    return div()
        .with(
            QuestionConfig.multiOptionQuestionFieldTemplate(messages)
                .withId("multi-option-question-answer-template")
                // Add "hidden" to other classes, so that the template is not shown
                .withClasses(
                    ReferenceClasses.MULTI_OPTION_QUESTION_OPTION,
                    ReferenceClasses.MULTI_OPTION_QUESTION_OPTION_EDITABLE,
                    "hidden",
                    "flex",
                    "flex-row",
                    "mb-4",
                    "items-center"));
  }

  private FormTag buildNewQuestionForm(
      QuestionForm questionForm,
      ImmutableList<EnumeratorQuestionDefinition> enumeratorQuestionDefinitions,
      Request request) {
    SelectWithLabel enumeratorOptions =
        enumeratorOptionsFromEnumerationQuestionDefinitions(
            questionForm, enumeratorQuestionDefinitions);
    String cancelUrl = questionForm.getRedirectUrl();
    if (Strings.isNullOrEmpty(cancelUrl)) {
      cancelUrl = controllers.admin.routes.AdminQuestionController.index().url();
    }
    FormTag formTag = buildSubmittableQuestionForm(questionForm, enumeratorOptions, true, request);
    formTag
        .withAction(
            controllers.admin.routes.AdminQuestionController.create(
                    questionForm.getQuestionType().toString())
                .url())
        .with(
            div()
                .withClasses("flex", "space-x-2", "mt-3")
                .with(
                    div().withClasses("flex-grow"),
                    asRedirectElement(button("Cancel"), cancelUrl)
                        .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON),
                    submitButton("Create").withClass("m-4").withClasses(ButtonStyles.SOLID_BLUE)));

    return formTag;
  }

  private FormTag buildEditQuestionForm(
      long id,
      QuestionForm questionForm,
      Optional<QuestionDefinition> maybeEnumerationQuestionDefinition,
      Request request,
      Modal unsetUniversalModal) {
    SelectWithLabel enumeratorOption =
        enumeratorOptionsFromMaybeEnumerationQuestionDefinition(maybeEnumerationQuestionDefinition);
    FormTag formTag = buildSubmittableQuestionForm(questionForm, enumeratorOption, false, request);
    formTag.withAction(
        controllers.admin.routes.AdminQuestionController.update(
                id, questionForm.getQuestionType().toString())
            .url());

    if (settingsManifest.getUniversalQuestions(request)) {
      formTag.with(unsetUniversalModal.getButton());
    } else {
      formTag.with(submitButton("Update").withClasses("ml-2", ButtonStyles.SOLID_BLUE));
    }

    return formTag;
  }

  private Modal buildUnsetUniversalModal(QuestionForm questionForm) {
    ButtonTag triggerModalButton = button("Update").withClasses("ml-2", ButtonStyles.SOLID_BLUE);
    FormTag confirmUnsetUniversalForm =
        form()
            .with(
                div("This question will no longer be set as a recommended "
                        + questionForm.getQuestionType().getLabel()
                        + " question.")
                    .withClasses("mb-8"),
                div(
                        submitButton("Remove from universal questions")
                            .withId("accept-question-updates-button")
                            .attr("form", "full-edit-form")
                            .withClasses(ButtonStyles.SOLID_BLUE),
                        button("Cancel")
                            .withClasses(ButtonStyles.LINK_STYLE, ReferenceClasses.MODAL_CLOSE))
                    .withClasses("flex", "flex-col", StyleUtils.responsiveMedium("flex-row")));
    return Modal.builder()
        .setModalId("confirm-question-updates-modal")
        .setLocation(Modal.Location.ADMIN_FACING)
        .setContent(confirmUnsetUniversalForm)
        .setModalTitle(
            "Are you sure you want to remove this question from the universal questions set?")
        .setTriggerButtonContent(triggerModalButton)
        .setWidth(Modal.Width.THIRD)
        .build();
  }

  private FormTag buildQuestionForm(
      QuestionForm questionForm,
      SelectWithLabel enumeratorOptions,
      boolean submittable,
      boolean forCreate,
      Request request) {
    QuestionType questionType = questionForm.getQuestionType();
    FormTag formTag =
        form()
            .withId("full-edit-form")
            .withMethod("POST")
            .with(
                // Hidden input indicating the type of question to be created.
                input().isHidden().withName("questionType").withValue(questionType.name()),
                input()
                    .isHidden()
                    .withName(QuestionForm.REDIRECT_URL_PARAM)
                    .withValue(questionForm.getRedirectUrl()),
                requiredFieldsExplanationContent());
    formTag.with(
        h2("Visible to applicants").withClasses("py-2", "mt-6", "font-semibold"),
        repeatedQuestionInformation(),
        FieldWithLabel.textArea()
            .setId("question-text-textarea")
            .setFieldName("questionText")
            .setLabelText("Question text displayed to the applicant")
            .setRequired(true)
            .setDisabled(!submittable)
            .setValue(questionForm.getQuestionText())
            .getTextareaTag());
    if (!questionType.equals(QuestionType.STATIC)) {
      formTag.with(
          FieldWithLabel.textArea()
              .setId("question-help-text-textarea")
              .setFieldName("questionHelpText")
              .setLabelText("Question help text displayed to the applicant")
              .setDisabled(!submittable)
              .setValue(questionForm.getQuestionHelpText())
              .getTextareaTag());
    }

    // The question name and enumerator fields should not be changed after the question is created.
    // If this form is not for creation, hidden fields to pass enumerator and name data are added.
    formTag
        .with(
            h2("Visible to administrators only").withClasses("py-2", "mt-6", "font-semibold"),
            administrativeNameField(questionForm.getQuestionName(), !forCreate))
        .condWith(
            forCreate,
            p().withId("question-name-preview")
                .withClasses("text-xs", "text-gray-500", "pb-3")
                .with(span("Visible in the API as: "), span("").withId("formatted-name")));
    if (!forCreate) {
      formTag.with(
          input()
              .isHidden()
              .withName(QUESTION_ENUMERATOR_FIELD)
              .withValue(
                  questionForm
                      .getEnumeratorId()
                      .map(String::valueOf)
                      .orElse(NO_ENUMERATOR_ID_STRING)));
    }

    formTag.with(
        FieldWithLabel.textArea()
            .setFieldName("questionDescription")
            .setLabelText("Question note for administrative use only")
            .setDisabled(!submittable)
            .setValue(questionForm.getQuestionDescription())
            .getTextareaTag(),
        enumeratorOptions.setDisabled(!forCreate).getSelectTag());

    ImmutableList.Builder<DomContent> questionSettingsContentBuilder = ImmutableList.builder();
    Optional<DivTag> questionConfig = QuestionConfig.buildQuestionConfig(questionForm, messages);
    if (questionConfig.isPresent()) {
      questionSettingsContentBuilder.add(questionConfig.get());
    }
    if (settingsManifest.getUniversalQuestions(request)) {
      questionSettingsContentBuilder.add(buildUniversalQuestion(questionForm));
    }
    if (settingsManifest.getPrimaryApplicantInfoQuestions(request)
        && questionForm.getEnumeratorId().isEmpty()
        && PrimaryApplicantInfoTag.getAllQuestionTypes().contains(questionType)) {
      questionSettingsContentBuilder.add(buildPrimaryApplicantInfoSection(questionForm));
    }
    if (!CsvExporterService.NON_EXPORTED_QUESTION_TYPES.contains(questionType)) {
      questionSettingsContentBuilder.add(buildDemographicFields(questionForm, submittable));
    }

    ImmutableList<DomContent> questionSettingsContent = questionSettingsContentBuilder.build();
    if (!questionSettingsContent.isEmpty()) {
      formTag
          .with(h2("Question settings").withClasses("py-2", "mt-6", "font-semibold"))
          .with(questionSettingsContent);
    }

    return formTag;
  }

  private DomContent buildUniversalQuestion(QuestionForm questionForm) {
    return fieldset()
        .with(
            legend("Universal question").withClass(BaseStyles.INPUT_LABEL),
            p().withClasses("px-1", "pb-2", "text-sm", "text-gray-600")
                .with(
                    span(
                        "Universal questions will be recommended as a standardized type of"
                            + " question to be added to all programs.")),
            ViewUtils.makeToggleButton(
                /* fieldName= */ "isUniversal",
                /* enabled= */ questionForm.isUniversal(),
                /* hidden= */ false,
                /* idPrefix= */ Optional.of("universal"),
                /* text= */ Optional.of("Set as a universal question")));
  }

  private FieldsetTag buildPrimaryApplicantInfoSection(QuestionForm questionForm) {
    FieldsetTag result =
        fieldset()
            .withId("primary-applicant-info")
            .with(legend("Primary Applicant Information").withClass(BaseStyles.INPUT_LABEL));
    PrimaryApplicantInfoTag.getAllTagsForQuestionType(questionForm.getQuestionType())
        .forEach(
            primaryApplicantInfoTag -> {
              Optional<QuestionDefinition> currentQuestionForTag =
                  questionService.getReadOnlyQuestionServiceSync().getUpToDateQuestions().stream()
                      .filter(
                          qd -> qd.getPrimaryApplicantInfoTags().contains(primaryApplicantInfoTag))
                      .findFirst();
              boolean differentQuestionHasTag =
                  currentQuestionForTag
                      .map(question -> !question.getName().equals(questionForm.getQuestionName()))
                      .orElse(false);
              DivTag tagSubsection =
                  div()
                      .withClass("cf-primary-applicant-info-subsection")
                      .with(
                          p().withClasses("px-1", "pb-2", "text-sm", "text-gray-600")
                              .with(
                                  span(primaryApplicantInfoTag.getDescription()),
                                  ViewUtils.makeToggleButton(
                                      /* fieldName= */ primaryApplicantInfoTag.getFieldName(),
                                      /* enabled= */ questionForm
                                          .primaryApplicantInfoTags()
                                          .contains(primaryApplicantInfoTag),
                                      /* hidden= */ !questionForm.isUniversal()
                                          || differentQuestionHasTag,
                                      /* idPrefix= */ Optional.of(
                                          primaryApplicantInfoTag.getFieldName()),
                                      /* text= */ Optional.of(
                                          primaryApplicantInfoTag.getDisplayName()))));
              tagSubsection.with(
                  ViewUtils.makeAlertInfoSlim(
                      "You cannot edit this setting since the question is not a universal"
                          + " question.",
                      /* hidden= */ questionForm.isUniversal(),
                      /* classes...= */ "cf-primary-applicant-info-universal-alert",
                      "mb-4",
                      "usa-alert-primary-applicant-info"));
              tagSubsection.condWith(
                  differentQuestionHasTag,
                  ViewUtils.makeAlertInfoSlim(
                      String.format(
                          "You cannot edit this setting since this property is already set on a"
                              + " question named %s.",
                          currentQuestionForTag.map(QuestionDefinition::getName).orElse("")),
                      /* hidden= */ false,
                      /* classes...= */ "cf-primary-applicant-info-tag-already-set-alert",
                      "mb-4",
                      "usa-alert-primary-applicant-info"));
              result.with(tagSubsection);
            });
    return result;
  }

  private DomContent buildDemographicFields(QuestionForm questionForm, boolean submittable) {

    QuestionTag exportState = questionForm.getQuestionExportStateTag();
    // TODO(#2618): Consider using helpers for grouping related radio controls.
    return fieldset()
        .with(
            legend("Demographic data privacy settings")
                .with(ViewUtils.requiredQuestionIndicator())
                .withClass(BaseStyles.INPUT_LABEL),
            p().withClasses("px-1", "pb-2", "text-sm", "text-gray-600")
                .with(
                    span(
                        "The setting below only controls what is exported in the demographic"
                            + " export. Answers are always exported via the API.")),
            FieldWithLabel.radio()
                .setDisabled(!submittable)
                .setAriaRequired(true)
                .setFieldName("questionExportState")
                .setLabelText("Don't include in demographic export")
                .setValue(QuestionTag.NON_DEMOGRAPHIC.getValue())
                .setChecked(exportState == QuestionTag.NON_DEMOGRAPHIC)
                .getRadioTag(),
            FieldWithLabel.radio()
                .setDisabled(!submittable)
                .setAriaRequired(true)
                .setFieldName("questionExportState")
                .setLabelText("Include in demographic export")
                .setValue(QuestionTag.DEMOGRAPHIC.getValue())
                .setChecked(exportState == QuestionTag.DEMOGRAPHIC)
                .getRadioTag(),
            FieldWithLabel.radio()
                .setDisabled(!submittable)
                .setAriaRequired(true)
                .setFieldName("questionExportState")
                .setLabelText("Obfuscate and include in demographic export")
                .setValue(QuestionTag.DEMOGRAPHIC_PII.getValue())
                .setChecked(exportState == QuestionTag.DEMOGRAPHIC_PII)
                .getRadioTag(),
            span("Learn more about each of the demographic data export settings in the "),
            new LinkElement()
                .setHref(
                    "https://docs.civiform.us/user-manual/civiform-admin-guide/manage-questions#question-export-settings")
                .setText("documentation")
                .opensInNewTab()
                .asAnchorText(),
            span("."));
  }

  /**
   * Generate a {@link SelectWithLabel} enumerator selector with all the available enumerator
   * question definitions.
   */
  private SelectWithLabel enumeratorOptionsFromEnumerationQuestionDefinitions(
      QuestionForm questionForm,
      ImmutableList<EnumeratorQuestionDefinition> enumeratorQuestionDefinitions) {
    ImmutableList<SelectWithLabel.OptionValue> options =
        ImmutableList.<SelectWithLabel.OptionValue>builder()
            .add(
                SelectWithLabel.OptionValue.builder()
                    .setLabel(NO_ENUMERATOR_DISPLAY_STRING)
                    .setValue(NO_ENUMERATOR_ID_STRING)
                    .build())
            .addAll(
                enumeratorQuestionDefinitions.stream()
                    .map(
                        q ->
                            SelectWithLabel.OptionValue.builder()
                                .setLabel(q.getName())
                                .setValue(String.valueOf(q.getId()))
                                .build())
                    .collect(ImmutableList.toImmutableList()))
            .build();
    return enumeratorOptions(
        options,
        questionForm.getEnumeratorId().map(String::valueOf).orElse(NO_ENUMERATOR_ID_STRING));
  }

  /**
   * Generate a {@link SelectWithLabel} enumerator selector with one value from an enumerator
   * question definition if available, or with just the no-enumerator option.
   */
  private SelectWithLabel enumeratorOptionsFromMaybeEnumerationQuestionDefinition(
      Optional<QuestionDefinition> maybeEnumerationQuestionDefinition) {
    String enumeratorName =
        maybeEnumerationQuestionDefinition
            .map(QuestionDefinition::getName)
            .orElse(NO_ENUMERATOR_DISPLAY_STRING);
    String enumeratorId =
        maybeEnumerationQuestionDefinition
            .map(q -> String.valueOf(q.getId()))
            .orElse(NO_ENUMERATOR_ID_STRING);
    return enumeratorOptions(
        ImmutableList.of(
            SelectWithLabel.OptionValue.builder()
                .setLabel(enumeratorName)
                .setValue(enumeratorId)
                .build()),
        enumeratorId);
  }

  private SelectWithLabel enumeratorOptions(
      ImmutableList<SelectWithLabel.OptionValue> options, String selected) {
    return new SelectWithLabel()
        .setId("question-enumerator-select")
        .setFieldName(QUESTION_ENUMERATOR_FIELD)
        .setLabelText("Question enumerator")
        .setOptions(options)
        .setValue(selected)
        .setRequired(true);
  }

  private DivTag repeatedQuestionInformation() {
    return div("By selecting an enumerator, you are creating a repeated question - a question that"
            + " is asked for each repeated entity enumerated by the applicant. Please"
            + " reference the applicant-defined repeated entity name to give the applicant"
            + " context on which repeated entity they are answering the question for by"
            + " using \"$this\" in the question's text and help text. To reference the"
            + " repeated entities containing this one, use \"$this.parent\","
            + " \"this.parent.parent\", etc.")
        .withId("repeated-question-information")
        .withClasses(
            "hidden",
            "text-blue-500",
            "text-sm",
            "p-2",
            "font-mono",
            "border-4",
            "border-blue-400");
  }

  private DivTag administrativeNameField(String adminName, boolean editExistingQuestion) {
    if (editExistingQuestion) {
      return div()
          .withClass("mb-2")
          .with(
              p("Administrative identifier. This value can't be changed")
                  .withClasses(BaseStyles.INPUT_LABEL),
              p(adminName).withId("question-name-input").withClasses(BaseStyles.FORM_FIELD),
              input().isHidden().withName(QUESTION_NAME_FIELD).withValue(adminName));
    }
    return FieldWithLabel.input()
        .setId("question-name-input")
        .setFieldName(QUESTION_NAME_FIELD)
        .setLabelText("Administrative identifier. This value can't be changed later")
        .setToolTipText(
            "This will be used to identify questions in the API and CSV export.  It will be"
                + " formatted so that white spaces are replaced with underscores, uppercase"
                + " letters are converted to lowercase and all non-alphabetic characters are"
                + " stripped.")
        .setToolTipIcon(Icons.INFO)
        .setRequired(true)
        .setValue(adminName)
        .getInputTag();
  }
}
