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
import forms.MultiOptionQuestionForm;
import forms.QuestionForm;
import forms.QuestionFormBuilder;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import java.util.Optional;
import models.QuestionTag;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.export.ExporterService;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.BaseHtmlView;
import views.FileUploadViewStrategy;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.FieldWithLabel;
import views.components.LinkElement;
import views.components.SelectWithLabel;
import views.components.ToastMessage;
import views.style.AdminStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.Styles;

/** Renders a page for editing a question. */
public final class QuestionEditView extends BaseHtmlView {
  private final AdminLayout layout;
  private final Messages messages;
  private final FileUploadViewStrategy fileUploadViewStrategy;

  private static final String NO_ENUMERATOR_DISPLAY_STRING = "does not repeat";
  private static final String NO_ENUMERATOR_ID_STRING = "";
  private static final String QUESTION_NAME_FIELD = "questionName";
  private static final String QUESTION_ENUMERATOR_FIELD = "enumeratorId";

  // Setting a value of 0 causes the toast to be displayed indefinitely.
  private static final int ERROR_TOAST_DURATION = 0;

  @Inject
  public QuestionEditView(
      AdminLayoutFactory layoutFactory,
      MessagesApi messagesApi,
      FileUploadViewStrategy fileUploadViewStrategy) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.QUESTIONS);
    // Use the default language for CiviForm, since this is an admin view and not applicant-facing.
    this.messages = messagesApi.preferred(ImmutableList.of(Lang.defaultLang()));
    this.fileUploadViewStrategy = checkNotNull(fileUploadViewStrategy);
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
    String title = String.format("New %s question", questionType.getLabel().toLowerCase());

    DivTag formContent =
        buildQuestionContainer(title, questionForm)
            .with(
                buildNewQuestionForm(questionForm, enumeratorQuestionDefinitions)
                    .with(makeCsrfTokenInputTag(request)));

    message
        .map(m -> m.setDismissible(true))
        .map(m -> m.setDuration(ERROR_TOAST_DURATION))
        .map(ToastMessage::getContainerTag)
        .ifPresent(formContent::with);

    return renderWithPreview(formContent, questionType, title);
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

    QuestionType questionType = questionForm.getQuestionType();
    String title = String.format("Edit %s question", questionType.getLabel().toLowerCase());

    DivTag formContent =
        buildQuestionContainer(title, questionForm)
            .with(
                buildEditQuestionForm(id, questionForm, maybeEnumerationQuestionDefinition)
                    .with(makeCsrfTokenInputTag(request)));

    message
        .map(m -> m.setDismissible(true))
        .map(m -> m.setDuration(ERROR_TOAST_DURATION))
        .map(ToastMessage::getContainerTag)
        .ifPresent(formContent::with);

    return renderWithPreview(formContent, questionType, title);
  }

  /** Render a read-only non-submittable question form. */
  public Content renderViewQuestionForm(
      QuestionDefinition questionDefinition,
      Optional<QuestionDefinition> maybeEnumerationQuestionDefinition)
      throws InvalidQuestionTypeException {
    QuestionForm questionForm = QuestionFormBuilder.create(questionDefinition);
    QuestionType questionType = questionForm.getQuestionType();
    String title = String.format("View %s question", questionType.toString().toLowerCase());

    SelectWithLabel enumeratorOption =
        enumeratorOptionsFromMaybeEnumerationQuestionDefinition(maybeEnumerationQuestionDefinition);
    DivTag formContent =
        buildQuestionContainer(title, QuestionFormBuilder.create(questionDefinition))
            .with(buildViewOnlyQuestionForm(questionForm, enumeratorOption));

    return renderWithPreview(formContent, questionType, title);
  }

  private Content renderWithPreview(DivTag formContent, QuestionType type, String title) {
    DivTag previewContent =
        QuestionPreview.renderQuestionPreview(type, messages, fileUploadViewStrategy);

    HtmlBundle htmlBundle =
        layout.getBundle().setTitle(title).addMainContent(formContent, previewContent);
    return layout.render(htmlBundle);
  }

  private FormTag buildSubmittableQuestionForm(
      QuestionForm questionForm, SelectWithLabel enumeratorOptions, boolean forCreate) {
    return buildQuestionForm(questionForm, enumeratorOptions, true, forCreate);
  }

  private FormTag buildViewOnlyQuestionForm(
      QuestionForm questionForm, SelectWithLabel enumeratorOptions) {
    return buildQuestionForm(questionForm, enumeratorOptions, false, false);
  }

  private DivTag buildQuestionContainer(String title, QuestionForm questionForm) {
    return div()
        .withId("question-form")
        .withClasses(
            Styles.BORDER_GRAY_400,
            Styles.BORDER_R,
            Styles.P_6,
            Styles.FLEX,
            Styles.FLEX_COL,
            Styles.H_FULL,
            Styles.OVERFLOW_HIDDEN,
            Styles.OVERFLOW_Y_AUTO,
            Styles.RELATIVE,
            Styles.W_2_5)
        .with(renderHeader(title))
        .with(multiOptionQuestionField(questionForm));
  }

  // A hidden template for multi-option questions.
  private DivTag multiOptionQuestionField(QuestionForm questionForm) {
    DivTag multiOptionQuestionField =
        div()
            .with(
                QuestionConfig.multiOptionQuestionFieldTemplate(messages)
                    .withId("multi-option-question-answer-template")
                    // Add "hidden" to other classes, so that the template is not shown
                    .withClasses(
                        ReferenceClasses.MULTI_OPTION_QUESTION_OPTION,
                        Styles.HIDDEN,
                        Styles.FLEX,
                        Styles.FLEX_ROW,
                        Styles.MB_4));
    if (questionForm instanceof MultiOptionQuestionForm) {
      multiOptionQuestionField.with(
          FieldWithLabel.number()
              .setFieldName("nextAvailableId")
              .setValue(((MultiOptionQuestionForm) questionForm).getNextAvailableId())
              .getNumberTag()
              .withClasses(Styles.HIDDEN));
    }
    return multiOptionQuestionField;
  }

  private FormTag buildNewQuestionForm(
      QuestionForm questionForm,
      ImmutableList<EnumeratorQuestionDefinition> enumeratorQuestionDefinitions) {
    SelectWithLabel enumeratorOptions =
        enumeratorOptionsFromEnumerationQuestionDefinitions(
            questionForm, enumeratorQuestionDefinitions);
    String cancelUrl = questionForm.getRedirectUrl();
    if (Strings.isNullOrEmpty(cancelUrl)) {
      cancelUrl = controllers.admin.routes.AdminQuestionController.index().url();
    }
    FormTag formTag = buildSubmittableQuestionForm(questionForm, enumeratorOptions, true);
    formTag
        .withAction(
            controllers.admin.routes.AdminQuestionController.create(
                    questionForm.getQuestionType().toString())
                .url())
        .with(
            div()
                .withClasses(Styles.FLEX, Styles.SPACE_X_2, Styles.MT_3)
                .with(
                    div().withClasses(Styles.FLEX_GROW),
                    asRedirectElement(button("Cancel"), questionForm.getRedirectUrl())
                        .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES),
                    submitButton("Create")
                        .withClass(Styles.M_4)
                        .withClasses(AdminStyles.PRIMARY_BUTTON_STYLES)));

    return formTag;
  }

  private FormTag buildEditQuestionForm(
      long id,
      QuestionForm questionForm,
      Optional<QuestionDefinition> maybeEnumerationQuestionDefinition) {
    SelectWithLabel enumeratorOption =
        enumeratorOptionsFromMaybeEnumerationQuestionDefinition(maybeEnumerationQuestionDefinition);
    FormTag formTag = buildSubmittableQuestionForm(questionForm, enumeratorOption, false);
    formTag
        .withAction(
            controllers.admin.routes.AdminQuestionController.update(
                    id, questionForm.getQuestionType().toString())
                .url())
        .with(submitButton("Update").withClass(Styles.ML_2));
    return formTag;
  }

  private FormTag buildQuestionForm(
      QuestionForm questionForm,
      SelectWithLabel enumeratorOptions,
      boolean submittable,
      boolean forCreate) {
    QuestionType questionType = questionForm.getQuestionType();
    FormTag formTag =
        form()
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
        h2("Visible to applicants").withClasses(Styles.PY_2),
        repeatedQuestionInformation(),
        FieldWithLabel.textArea()
            .setId("question-text-textarea")
            .setFieldName("questionText")
            .setLabelText("Question text*")
            .setPlaceholderText("The question text displayed to the applicant")
            .setDisabled(!submittable)
            .setValue(questionForm.getQuestionText())
            .getTextareaTag(),
        FieldWithLabel.textArea()
            .setId("question-help-text-textarea")
            .setFieldName("questionHelpText")
            .setLabelText("Question help text")
            .setPlaceholderText("The question help text displayed to the applicant")
            .setDisabled(!submittable)
            .setValue(questionForm.getQuestionHelpText())
            .getTextareaTag()
            .withCondClass(questionType.equals(QuestionType.STATIC), Styles.HIDDEN));

    // The question name and enumerator fields should not be changed after the question is created.
    // If this form is not for creation, the fields are disabled, and hidden fields to pass
    // enumerator and name data are added.
    formTag.with(h2("Visible to administrators only").withClasses(Styles.PY_2));
    FieldWithLabel nameField =
        FieldWithLabel.input()
            .setId("question-name-input")
            .setFieldName(QUESTION_NAME_FIELD)
            .setLabelText("Administrative identifier. This value cannot be changed later*")
            .setDisabled(!submittable)
            .setValue(questionForm.getQuestionName());
    formTag.with(nameField.setDisabled(!forCreate).getInputTag());
    if (!forCreate) {
      formTag.with(
          input()
              .isHidden()
              .withName(QUESTION_NAME_FIELD)
              .withValue(questionForm.getQuestionName()),
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

    if (!ExporterService.NON_EXPORTED_QUESTION_TYPES.contains(questionType)) {
      questionSettingsContentBuilder.add(buildDemographicFields(questionForm, submittable));
    }
    ImmutableList<DomContent> questionSettingsContent = questionSettingsContentBuilder.build();
    if (!questionSettingsContent.isEmpty()) {
      formTag.with(h2("Question settings").withClasses(Styles.PY_2)).with(questionSettingsContent);
    }

    return formTag;
  }

  private DomContent buildDemographicFields(QuestionForm questionForm, boolean submittable) {

    QuestionTag exportState = questionForm.getQuestionExportStateTag();
    // TODO(#2618): Consider using helpers for grouping related radio controls.
    return fieldset()
        .with(
            legend("Data privacy settings*").withClass(BaseStyles.INPUT_LABEL),
            p().withClasses(Styles.PX_1, Styles.PB_2, Styles.TEXT_SM, Styles.TEXT_GRAY_600)
                .with(
                    span("Learn more about each of the data export settings in the "),
                    new LinkElement()
                        .setHref(
                            "https://docs.civiform.us/user-manual/civiform-admin-guide/manage-questions#question-export-settings")
                        .setText("documentation")
                        .opensInNewTab()
                        .asAnchorText(),
                    span(".")),
            FieldWithLabel.radio()
                .setDisabled(!submittable)
                .setFieldName("questionExportState")
                .setLabelText("Don't allow answers to be exported")
                .setValue(QuestionTag.NON_DEMOGRAPHIC.getValue())
                .setChecked(exportState == QuestionTag.NON_DEMOGRAPHIC)
                .getRadioTag(),
            FieldWithLabel.radio()
                .setDisabled(!submittable)
                .setFieldName("questionExportState")
                .setLabelText("Export exact answers")
                .setValue(QuestionTag.DEMOGRAPHIC.getValue())
                .setChecked(exportState == QuestionTag.DEMOGRAPHIC)
                .getRadioTag(),
            FieldWithLabel.radio()
                .setDisabled(!submittable)
                .setFieldName("questionExportState")
                .setLabelText("Export obfuscated answers")
                .setValue(QuestionTag.DEMOGRAPHIC_PII.getValue())
                .setChecked(exportState == QuestionTag.DEMOGRAPHIC_PII)
                .getRadioTag());
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
        .setLabelText("Question enumerator*")
        .setOptions(options)
        .setValue(selected);
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
            Styles.HIDDEN,
            Styles.TEXT_BLUE_500,
            Styles.TEXT_SM,
            Styles.P_2,
            Styles.FONT_MONO,
            Styles.BORDER_4,
            Styles.BORDER_BLUE_400);
  }
}
