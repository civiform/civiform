package views.admin.questions;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;

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
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http.Request;
import play.twirl.api.Content;
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
import views.style.Styles;

public final class QuestionEditView extends BaseHtmlView {
  private final AdminLayout layout;
  private final Messages messages;

  private static final String NO_ENUMERATOR_DISPLAY_STRING = "does not repeat";
  private static final String NO_ENUMERATOR_ID_STRING = "";

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
    //    String title = String.format("New %s question", questionType.toString().toLowerCase());
    String title = String.format("New %s question", questionType.toString().toLowerCase());

    ContainerTag formContent =
        buildQuestionContainer(title)
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
        buildQuestionContainer(title)
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
        buildQuestionContainer(title)
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
      QuestionForm questionForm, SelectWithLabel enumeratorOptions) {
    return buildQuestionForm(questionForm, enumeratorOptions, true);
  }

  private ContainerTag buildViewOnlyQuestionForm(
      QuestionForm questionForm, SelectWithLabel enumeratorOptions) {
    return buildQuestionForm(questionForm, enumeratorOptions, false);
  }

  private ContainerTag buildQuestionContainer(String title) {
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
      ImmutableList<EnumeratorQuestionDefinition> enumeratorQuestionDefinitions) {
    SelectWithLabel enumeratorOptions =
        enumeratorOptionsFromEnumerationQuestionDefinitions(
            questionForm, enumeratorQuestionDefinitions);
    ContainerTag formTag = buildSubmittableQuestionForm(questionForm, enumeratorOptions);
    formTag
        .withAction(
            controllers.admin.routes.AdminQuestionController.create(
                    questionForm.getQuestionType().toString())
                .url())
        .with(submitButton("Create").withClass(Styles.ML_2));

    return formTag;
  }

  private ContainerTag buildEditQuestionForm(
      long id,
      QuestionForm questionForm,
      Optional<QuestionDefinition> maybeEnumerationQuestionDefinition) {
    SelectWithLabel enumeratorOption =
        enumeratorOptionsFromMaybeEnumerationQuestionDefinition(maybeEnumerationQuestionDefinition);
    ContainerTag formTag = buildSubmittableQuestionForm(questionForm, enumeratorOption);
    formTag
        .withAction(
            controllers.admin.routes.AdminQuestionController.update(
                    id, questionForm.getQuestionType().toString())
                .url())
        .with(submitButton("Update").withClass(Styles.ML_2));
    return formTag;
  }

  private ContainerTag buildQuestionForm(
      QuestionForm questionForm, SelectWithLabel enumeratorOptions, boolean submittable) {
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
            enumeratorOptions.setDisabled(!submittable).getContainer(),
            repeatedQuestionInformation(),
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
   * Generate a {@link SelectWithLabel} enumerator selector with all the available enumerator
   * question definitions.
   */
  private SelectWithLabel enumeratorOptionsFromEnumerationQuestionDefinitions(
      QuestionForm questionForm,
      ImmutableList<EnumeratorQuestionDefinition> enumeratorQuestionDefinitions) {
    ImmutableList.Builder<SimpleEntry<String, String>> optionsBuilder = ImmutableList.builder();
    optionsBuilder.add(new SimpleEntry<>(NO_ENUMERATOR_DISPLAY_STRING, NO_ENUMERATOR_ID_STRING));
    optionsBuilder.addAll(
        enumeratorQuestionDefinitions.stream()
            .map(
                enumeratorQuestionDefinition ->
                    new SimpleEntry<>(
                        enumeratorQuestionDefinition.getName(),
                        String.valueOf(enumeratorQuestionDefinition.getId())))
            .collect(ImmutableList.toImmutableList()));
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
    SimpleEntry<String, String> enumeratorOption =
        maybeEnumerationQuestionDefinition
            .map(
                enumeratorQuestionDefinition ->
                    new SimpleEntry<>(
                        enumeratorQuestionDefinition.getName(),
                        String.valueOf(enumeratorQuestionDefinition.getId())))
            .orElse(new SimpleEntry<>(NO_ENUMERATOR_DISPLAY_STRING, NO_ENUMERATOR_ID_STRING));
    return enumeratorOptions(ImmutableList.of(enumeratorOption), enumeratorOption.getValue());
  }

  private SelectWithLabel enumeratorOptions(
      ImmutableList<SimpleEntry<String, String>> options, String selected) {
    return new SelectWithLabel()
        .setId("question-enumerator-select")
        .setFieldName("enumeratorId")
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
