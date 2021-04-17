package views.admin.questions;

import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.main;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import forms.QuestionForm;
import forms.QuestionFormBuilder;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Optional;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.question.types.RepeaterQuestionDefinition;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;
import views.components.SelectWithLabel;
import views.components.ToastMessage;
import views.style.Styles;

public final class QuestionEditView extends BaseHtmlView {
  private final AdminLayout layout;

  private static final String NO_REPEATER_DISPLAY_STRING = "does not repeat";
  private static final String NO_REPEATER_ID_STRING = "";

  @Inject
  public QuestionEditView(AdminLayout layout) {
    this.layout = layout;
  }

  /** Render a fresh New Question Form. */
  public Content renderNewQuestionForm(
      Request request,
      QuestionType questionType,
      ImmutableList<RepeaterQuestionDefinition> repeaterQuestionDefinitions)
      throws UnsupportedQuestionTypeException {
    QuestionForm questionForm = getFreshQuestionForm(questionType);
    return renderNewQuestionForm(
        request, questionForm, repeaterQuestionDefinitions, Optional.empty());
  }

  /** Render a New Question Form with a partially filled form and an error message. */
  public Content renderNewQuestionForm(
      Request request,
      QuestionForm questionForm,
      ImmutableList<RepeaterQuestionDefinition> repeaterQuestionDefinitions,
      String errorMessage) {
    return renderNewQuestionForm(
        request, questionForm, repeaterQuestionDefinitions, Optional.of(errorMessage));
  }

  private Content renderNewQuestionForm(
      Request request,
      QuestionForm questionForm,
      ImmutableList<RepeaterQuestionDefinition> repeaterQuestionDefinitions,
      Optional<String> message) {
    QuestionType questionType = questionForm.getQuestionType();
    String title = String.format("New %s question", questionType.toString().toLowerCase());

    ContainerTag formContent =
        buildQuestionContainer(title)
            .with(
                buildNewQuestionForm(questionForm, repeaterQuestionDefinitions)
                    .with(makeCsrfTokenInputTag(request)));

    if (message.isPresent()) {
      formContent.with(ToastMessage.error(message.get()).setDismissible(false).getContainerTag());
    }

    return renderWithPreview(formContent, questionType);
  }

  /** Render a fresh Edit Question Form. */
  public Content renderEditQuestionForm(Request request, QuestionDefinition questionDefinition)
      throws InvalidQuestionTypeException {
    QuestionForm questionForm = getQuestionFormFromQuestionDefinition(questionDefinition);
    return renderEditQuestionForm(
        request, questionDefinition.getId(), questionForm, questionDefinition, Optional.empty());
  }

  /** Render a Edit Question form with errors. */
  public Content renderEditQuestionForm(
      Request request,
      long id,
      QuestionForm questionForm,
      QuestionDefinition questionDefinition,
      String message) {
    return renderEditQuestionForm(
        request, id, questionForm, questionDefinition, Optional.of(message));
  }

  private Content renderEditQuestionForm(
      Request request,
      long id,
      QuestionForm questionForm,
      QuestionDefinition questionDefinition,
      Optional<String> message) {

    QuestionType questionType = questionForm.getQuestionType();
    String title = String.format("Edit %s question", questionType.toString().toLowerCase());

    ContainerTag formContent =
        buildQuestionContainer(title)
            .with(
                buildEditQuestionForm(id, questionForm, questionDefinition)
                    .with(makeCsrfTokenInputTag(request)));

    if (message.isPresent()) {
      formContent.with(ToastMessage.error(message.get()).setDismissible(false).getContainerTag());
    }

    return renderWithPreview(formContent, questionType);
  }

  /** Render a read-only non-submittable question form. */
  public Content renderViewQuestionForm(QuestionDefinition questionDefinition)
      throws InvalidQuestionTypeException {
    QuestionForm questionForm = getQuestionFormFromQuestionDefinition(questionDefinition);
    QuestionType questionType = questionForm.getQuestionType();
    String title = String.format("View %s question", questionType.toString().toLowerCase());

    SelectWithLabel repeaterOption = repeaterOptionFromQuestionDefinition(questionDefinition);
    ContainerTag formContent =
        buildQuestionContainer(title).with(buildViewOnlyQuestionForm(questionForm, repeaterOption));

    return renderWithPreview(formContent, questionType);
  }

  private Content renderWithPreview(ContainerTag formContent, QuestionType type) {
    ContainerTag previewContent = QuestionPreview.renderQuestionPreview(type);
    previewContent.with(layout.viewUtils.makeLocalJsTag("preview"));
    return layout.renderFull(main(formContent, previewContent));
  }

  private ContainerTag buildSubmittableQuestionForm(
      QuestionForm questionForm, SelectWithLabel repeaterOptions) {
    return buildQuestionForm(questionForm, repeaterOptions, true);
  }

  private ContainerTag buildViewOnlyQuestionForm(
      QuestionForm questionForm, SelectWithLabel repeaterOptions) {
    return buildQuestionForm(questionForm, repeaterOptions, false);
  }

  private ContainerTag buildQuestionContainer(String title) {
    return div()
        .withId("question-form")
        .withClasses(
            Styles.BORDER_GRAY_400,
            Styles.BORDER_R,
            Styles.FLEX,
            Styles.FLEX_COL,
            Styles.H_FULL,
            Styles.OVERFLOW_HIDDEN,
            Styles.OVERFLOW_Y_AUTO,
            Styles.RELATIVE,
            Styles.W_2_5)
        .with(renderHeader(title, Styles.CAPITALIZE))
        .with(multiOptionQuestionField());
  }

  // A hidden template for multi-option questions.
  private ContainerTag multiOptionQuestionField() {
    return QuestionConfig.multiOptionQuestionField(Optional.empty())
        .withId("multi-option-question-answer-template")
        // Add "hidden" to other classes, so that the template is not shown
        .withClasses(Styles.HIDDEN, Styles.FLEX, Styles.FLEX_ROW, Styles.MB_4);
  }

  private ContainerTag buildNewQuestionForm(
      QuestionForm questionForm,
      ImmutableList<RepeaterQuestionDefinition> repeaterQuestionDefinitions) {
    SelectWithLabel repeaterOptions =
        repeaterOptionsFromRepeaterQuestions(questionForm, repeaterQuestionDefinitions);
    ContainerTag formTag = buildSubmittableQuestionForm(questionForm, repeaterOptions);
    formTag
        .withAction(
            controllers.admin.routes.QuestionController.create(
                    questionForm.getQuestionType().toString())
                .url())
        .with(submitButton("Create").withClass(Styles.ML_2));

    return formTag;
  }

  private ContainerTag buildEditQuestionForm(
      long id, QuestionForm questionForm, QuestionDefinition questionDefinition) {
    SelectWithLabel repeaterOption = repeaterOptionFromQuestionDefinition(questionDefinition);
    ContainerTag formTag = buildSubmittableQuestionForm(questionForm, repeaterOption);
    formTag
        .withAction(
            controllers.admin.routes.QuestionController.update(
                    id, questionForm.getQuestionType().toString())
                .url())
        .with(submitButton("Update").withClass(Styles.ML_2));
    return formTag;
  }

  private ContainerTag buildQuestionForm(
      QuestionForm questionForm, SelectWithLabel repeaterOptions, boolean submittable) {
    QuestionType questionType = questionForm.getQuestionType();
    ContainerTag formTag = form().withMethod("POST");
    FieldWithLabel nameField =
        FieldWithLabel.input()
            .setId("question-name-input")
            .setFieldName("questionName")
            .setLabelText("Name")
            .setDisabled(!submittable)
            .setPlaceholderText("The name displayed in the question builder")
            .setValue(questionForm.getQuestionName());
    if (Strings.isNullOrEmpty(questionForm.getQuestionName())) {
      formTag.with(nameField.getContainer());
    } else {
      // If there is already a name, we need to disable the `name` field but we
      // need to add a hidden input to send the same name as well.
      formTag.with(
          nameField.setDisabled(true).getContainer(),
          input().isHidden().withValue(questionForm.getQuestionName()).withName("questionName"));
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
            repeaterOptions.setDisabled(!submittable).getContainer(),
            FieldWithLabel.textArea()
                .setId("question-text-textarea")
                .setFieldName("questionText")
                .setLabelText("Question text")
                .setPlaceholderText("The question text displayed to the applicant")
                .setDisabled(!submittable)
                .setValue(questionForm.getQuestionText())
                .getContainer(),
            FieldWithLabel.textArea()
                .setId("question-help-text-textarea")
                .setFieldName("questionHelpText")
                .setLabelText("Question help text")
                .setPlaceholderText("The question help text displayed to the applicant")
                .setDisabled(!submittable)
                .setValue(questionForm.getQuestionHelpText())
                .getContainer())
        .with(formQuestionTypeSelect(questionType));

    formTag.with(QuestionConfig.buildQuestionConfig(questionForm));
    return formTag;
  }

  private DomContent formQuestionTypeSelect(QuestionType selectedType) {
    ImmutableList<SimpleEntry<String, String>> options =
        Arrays.stream(QuestionType.values())
            .map(item -> new SimpleEntry<>(item.toString(), item.name()))
            .collect(ImmutableList.toImmutableList());

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
   * Generate a {@link SelectWithLabel} fixed repeater selector with a single option that is the
   * question definition's repeater id.
   */
  private SelectWithLabel repeaterOptionFromQuestionDefinition(
      QuestionDefinition questionDefinition) {
    SimpleEntry<String, String> repeaterPathAndId =
        new SimpleEntry<>(
            repeaterDisplayString(questionDefinition),
            questionDefinition.getRepeaterId().map(String::valueOf).orElse(NO_REPEATER_ID_STRING));
    return repeaterOptions(ImmutableList.of(repeaterPathAndId), "not used");
  }

  /**
   * Generate a {@link SelectWithLabel} repeater selector with all the available repeater question
   * definitions.
   */
  private SelectWithLabel repeaterOptionsFromRepeaterQuestions(
      QuestionForm questionForm,
      ImmutableList<RepeaterQuestionDefinition> repeaterQuestionDefinitions) {
    ImmutableList.Builder<SimpleEntry<String, String>> optionsBuilder = ImmutableList.builder();
    optionsBuilder.add(new SimpleEntry<>(NO_REPEATER_DISPLAY_STRING, NO_REPEATER_ID_STRING));
    optionsBuilder.addAll(
        repeaterQuestionDefinitions.stream()
            .map(
                repeaterQuestionDefinition ->
                    new SimpleEntry<>(
                        repeaterDisplayString(repeaterQuestionDefinition),
                        String.valueOf(repeaterQuestionDefinition.getId())))
            .collect(ImmutableList.toImmutableList()));
    return repeaterOptions(
        optionsBuilder.build(),
        questionForm.getRepeaterId().map(String::valueOf).orElse(NO_REPEATER_ID_STRING));
  }

  private SelectWithLabel repeaterOptions(
      ImmutableList<SimpleEntry<String, String>> options, String selected) {
    return new SelectWithLabel()
        .setId("question-repeater-select")
        .setFieldName("repeaterId")
        .setLabelText("Question repeater")
        .setOptions(options)
        .setValue(selected);
  }

  private QuestionForm getFreshQuestionForm(QuestionType questionType)
      throws UnsupportedQuestionTypeException {
    return QuestionFormBuilder.create(questionType);
  }

  private QuestionForm getQuestionFormFromQuestionDefinition(QuestionDefinition questionDefinition)
      throws InvalidQuestionTypeException {
    return QuestionDefinitionBuilder.create(questionDefinition);
  }

  /** Selector option to display for a given RepeaterQuestionDefinition. */
  private String repeaterDisplayString(RepeaterQuestionDefinition repeaterQuestionDefinition) {
    return repeaterQuestionDefinition.getName();
  }

  /** Selector option to display for a QuestionDefinition's repeater. */
  private String repeaterDisplayString(QuestionDefinition questionDefinition) {
    // TODO(#673): if question definition doesn't have a path, this needs to be updated.
    return questionDefinition.getRepeaterId().isEmpty()
        ? NO_REPEATER_DISPLAY_STRING
        : questionDefinition.getPath().parentPath().withoutArrayReference().keyName();
  }
}
