package views.admin.questions;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import forms.MultiOptionQuestionForm;
import forms.QuestionForm;
import forms.QuestionFormBuilder;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import java.util.Arrays;
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
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;
import views.components.SelectWithLabel;
import views.components.ToastMessage;
import views.style.ReferenceClasses;
import views.style.Styles;

/** Renders a page for editing a question. */
public final class QuestionEditView extends BaseHtmlView {
  private final AdminLayout layout;
  private final Messages messages;

  private static final String NO_ENUMERATOR_DISPLAY_STRING = "does not repeat";
  private static final String NO_ENUMERATOR_ID_STRING = "";
  private static final String QUESTION_NAME_FIELD = "questionName";
  private static final String QUESTION_ENUMERATOR_FIELD = "enumeratorId";

  @Inject
  public QuestionEditView(AdminLayout layout, MessagesApi messagesApi) {
    this.layout = checkNotNull(layout);
    // Use the default language for CiviForm, since this is an admin view and not applicant-facing.
    this.messages = messagesApi.preferred(ImmutableList.of(Lang.defaultLang()));
  }

  /** Render a fresh New Question Form. */
  public Content renderNewQuestionForm(
      Request request,
      QuestionType questionType,
      ImmutableList<EnumeratorQuestionDefinition> enumeratorQuestionDefinitions)
      throws UnsupportedQuestionTypeException {
    QuestionForm questionForm = QuestionFormBuilder.create(questionType);
    return renderNewQuestionForm(
        request, questionForm, enumeratorQuestionDefinitions, Optional.empty());
  }

  /** Render a New Question Form with a partially filled form and an error message. */
  public Content renderNewQuestionForm(
      Request request,
      QuestionForm questionForm,
      ImmutableList<EnumeratorQuestionDefinition> enumeratorQuestionDefinitions,
      String errorMessage) {
    return renderNewQuestionForm(
        request, questionForm, enumeratorQuestionDefinitions, Optional.of(errorMessage));
  }

  private Content renderNewQuestionForm(
      Request request,
      QuestionForm questionForm,
      ImmutableList<EnumeratorQuestionDefinition> enumeratorQuestionDefinitions,
      Optional<String> message) {
    QuestionType questionType = questionForm.getQuestionType();
    String title = String.format("New %s question", questionType.toString().toLowerCase());

    ContainerTag formContent =
        buildQuestionContainer(title, questionForm)
            .with(
                buildNewQuestionForm(questionForm, enumeratorQuestionDefinitions)
                    .with(makeCsrfTokenInputTag(request)));

    if (message.isPresent()) {
      formContent.with(ToastMessage.error(message.get()).setDismissible(false).getContainerTag());
    }

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
      String message) {
    return renderEditQuestionForm(
        request, id, questionForm, maybeEnumerationQuestionDefinition, Optional.of(message));
  }

  private Content renderEditQuestionForm(
      Request request,
      long id,
      QuestionForm questionForm,
      Optional<QuestionDefinition> maybeEnumerationQuestionDefinition,
      Optional<String> message) {

    QuestionType questionType = questionForm.getQuestionType();
    String title = String.format("Edit %s question", questionType.toString().toLowerCase());

    ContainerTag formContent =
        buildQuestionContainer(title, questionForm)
            .with(
                buildEditQuestionForm(id, questionForm, maybeEnumerationQuestionDefinition)
                    .with(makeCsrfTokenInputTag(request)));

    if (message.isPresent()) {
      formContent.with(ToastMessage.error(message.get()).setDismissible(false).getContainerTag());
    }

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
    ContainerTag formContent =
        buildQuestionContainer(title, QuestionFormBuilder.create(questionDefinition))
            .with(buildViewOnlyQuestionForm(questionForm, enumeratorOption));

    return renderWithPreview(formContent, questionType, title);
  }

  private Content renderWithPreview(ContainerTag formContent, QuestionType type, String title) {
    ContainerTag previewContent = QuestionPreview.renderQuestionPreview(type, messages);

    HtmlBundle htmlBundle =
        layout.getBundle().setTitle(title).addMainContent(formContent, previewContent);
    return layout.render(htmlBundle);
  }

  private ContainerTag buildSubmittableQuestionForm(
      QuestionForm questionForm, SelectWithLabel enumeratorOptions, boolean forCreate) {
    return buildQuestionForm(questionForm, enumeratorOptions, true, forCreate);
  }

  private ContainerTag buildViewOnlyQuestionForm(
      QuestionForm questionForm, SelectWithLabel enumeratorOptions) {
    return buildQuestionForm(questionForm, enumeratorOptions, false, false);
  }

  private ContainerTag buildQuestionContainer(String title, QuestionForm questionForm) {
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
  private ContainerTag multiOptionQuestionField(QuestionForm questionForm) {
    ContainerTag multiOptionQuestionField =
        div()
            .with(
                QuestionConfig.multiOptionQuestionField(Optional.empty(), messages)
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
              .getContainer()
              .withClasses(Styles.HIDDEN));
    }
    return multiOptionQuestionField;
  }

  private ContainerTag buildNewQuestionForm(
      QuestionForm questionForm,
      ImmutableList<EnumeratorQuestionDefinition> enumeratorQuestionDefinitions) {
    SelectWithLabel enumeratorOptions =
        enumeratorOptionsFromEnumerationQuestionDefinitions(
            questionForm, enumeratorQuestionDefinitions);
    ContainerTag formTag = buildSubmittableQuestionForm(questionForm, enumeratorOptions, true);
    formTag
        .withAction(
            controllers.admin.routes.AdminQuestionController.create(
                    questionForm.getQuestionType().toString())
                .url())
        .with(submitButton("Create").withClass(Styles.M_4));

    return formTag;
  }

  private ContainerTag buildEditQuestionForm(
      long id,
      QuestionForm questionForm,
      Optional<QuestionDefinition> maybeEnumerationQuestionDefinition) {
    SelectWithLabel enumeratorOption =
        enumeratorOptionsFromMaybeEnumerationQuestionDefinition(maybeEnumerationQuestionDefinition);
    ContainerTag formTag = buildSubmittableQuestionForm(questionForm, enumeratorOption, false);
    formTag
        .withAction(
            controllers.admin.routes.AdminQuestionController.update(
                    id, questionForm.getQuestionType().toString())
                .url())
        .with(submitButton("Update").withClass(Styles.ML_2));
    return formTag;
  }

  private ContainerTag buildQuestionForm(
      QuestionForm questionForm,
      SelectWithLabel enumeratorOptions,
      boolean submittable,
      boolean forCreate) {
    QuestionType questionType = questionForm.getQuestionType();
    ContainerTag formTag = form().withMethod("POST");

    // The question enumerator and name fields should not be changed after the question is created.
    // If this form is not for creation, the fields are disabled, and hidden fields to pass
    // enumerator
    // and name data are added.
    formTag.with(enumeratorOptions.setDisabled(!forCreate).getContainer());
    formTag.with(repeatedQuestionInformation());
    FieldWithLabel nameField =
        FieldWithLabel.input()
            .setId("question-name-input")
            .setFieldName(QUESTION_NAME_FIELD)
            .setLabelText("Name")
            .setDisabled(!submittable)
            .setPlaceholderText("The name displayed in the question builder")
            .setValue(questionForm.getQuestionName());
    formTag.with(nameField.setDisabled(!forCreate).getContainer());
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

    ContainerTag questionHelpTextField =
        FieldWithLabel.textArea()
            .setId("question-help-text-textarea")
            .setFieldName("questionHelpText")
            .setLabelText("Question help text")
            .setPlaceholderText("The question help text displayed to the applicant")
            .setDisabled(!submittable)
            .setValue(questionForm.getQuestionHelpText())
            .getContainer();
    if (questionType.equals(QuestionType.STATIC)) { // Hide help text for static questions.
      questionHelpTextField.withClasses(Styles.HIDDEN);
    }

    formTag
        .with(
            FieldWithLabel.textArea()
                .setId("question-description-textarea")
                .setFieldName("questionDescription")
                .setLabelText("Description")
                .setPlaceholderText("The description displayed in the question builder")
                .setDisabled(!submittable)
                .setValue(questionForm.getQuestionDescription())
                .getContainer(),
            FieldWithLabel.textArea()
                .setId("question-text-textarea")
                .setFieldName("questionText")
                .setLabelText("Question text")
                .setPlaceholderText("The question text displayed to the applicant")
                .setDisabled(!submittable)
                .setValue(questionForm.getQuestionText())
                .getContainer(),
            questionHelpTextField)
        .with(formQuestionTypeSelect(questionType));

    formTag.with(QuestionConfig.buildQuestionConfig(questionForm, messages));

    if (!ExporterService.NON_EXPORTED_QUESTION_TYPES.contains(questionType)) {
      formTag.with(
          div().withId("demographic-field-content").with(buildDemographicFields(questionForm)));
    }

    return formTag;
  }

  private ImmutableList<DomContent> buildDemographicFields(QuestionForm questionForm) {
    return ImmutableList.of(
        FieldWithLabel.radio()
            .setId("question-demographic-no-export")
            .setFieldName("questionExportState")
            .setLabelText("No export")
            .setValue(QuestionTag.NON_DEMOGRAPHIC.getValue())
            .setChecked(
                questionForm
                    .getQuestionExportState()
                    .equals(QuestionTag.NON_DEMOGRAPHIC.getValue()))
            .getContainer(),
        FieldWithLabel.radio()
            .setId("question-demographic-export-demographic")
            .setFieldName("questionExportState")
            .setLabelText("Export Value")
            .setValue(QuestionTag.DEMOGRAPHIC.getValue())
            .setChecked(
                questionForm.getQuestionExportState().equals(QuestionTag.DEMOGRAPHIC.getValue()))
            .getContainer(),
        FieldWithLabel.radio()
            .setId("question-demographic-export-pii")
            .setFieldName("questionExportState")
            .setLabelText("Export Obfuscated")
            .setValue(QuestionTag.DEMOGRAPHIC_PII.getValue())
            .setChecked(
                questionForm
                    .getQuestionExportState()
                    .equals(QuestionTag.DEMOGRAPHIC_PII.getValue()))
            .getContainer());
  }

  private DomContent formQuestionTypeSelect(QuestionType selectedType) {
    ImmutableMap<String, String> options =
        Arrays.stream(QuestionType.values()).collect(toImmutableMap(Enum::toString, Enum::name));

    return new SelectWithLabel()
        .setId("question-type-select")
        .setFieldName("questionType")
        .setLabelText("Question type")
        .setOptions(options)
        .setValue(selectedType.name())
        .getContainer()
        .withClasses(Styles.HIDDEN);
  }

  /**
   * Generate a {@link SelectWithLabel} enumerator selector with all the available enumerator
   * question definitions.
   */
  private SelectWithLabel enumeratorOptionsFromEnumerationQuestionDefinitions(
      QuestionForm questionForm,
      ImmutableList<EnumeratorQuestionDefinition> enumeratorQuestionDefinitions) {
    ImmutableMap.Builder<String, String> optionsBuilder = ImmutableMap.builder();
    optionsBuilder.put(NO_ENUMERATOR_DISPLAY_STRING, NO_ENUMERATOR_ID_STRING);
    optionsBuilder.putAll(
        enumeratorQuestionDefinitions.stream()
            .collect(toImmutableMap(QuestionDefinition::getName, q -> String.valueOf(q.getId()))));
    return enumeratorOptions(
        optionsBuilder.build(),
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
    return enumeratorOptions(ImmutableMap.of(enumeratorName, enumeratorId), enumeratorId);
  }

  private SelectWithLabel enumeratorOptions(ImmutableMap<String, String> options, String selected) {
    return new SelectWithLabel()
        .setId("question-enumerator-select")
        .setFieldName(QUESTION_ENUMERATOR_FIELD)
        .setLabelText("Question enumerator")
        .setOptions(options)
        .setValue(selected);
  }

  private ContainerTag repeatedQuestionInformation() {
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
