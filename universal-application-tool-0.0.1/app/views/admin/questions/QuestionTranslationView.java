package views.admin.questions;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import j2html.tags.ContainerTag;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Langs;
import play.mvc.Http;
import play.twirl.api.Content;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import views.admin.AdminLayout;
import views.admin.TranslationFormView;
import views.components.FieldWithLabel;

/** Renders a list of languages to select from, and a form for updating question information. */
public class QuestionTranslationView extends TranslationFormView {

  private final AdminLayout layout;

  @Inject
  public QuestionTranslationView(AdminLayout layout, Langs langs) {
    super(checkNotNull(langs));
    this.layout = checkNotNull(layout);
  }

  public Content render(Http.Request request, Locale locale, QuestionDefinition question) {
    return render(
        request,
        locale,
        question,
        question.getQuestionText().maybeGet(locale),
        question.getQuestionHelpText().maybeGet(locale),
        Optional.empty());
  }

  public Content renderErrors(
      Http.Request request, Locale locale, QuestionDefinition invalidQuestion, String errors) {
    return render(
        request,
        locale,
        invalidQuestion,
        invalidQuestion.getQuestionText().maybeGet(locale),
        invalidQuestion.getQuestionHelpText().maybeGet(locale),
        Optional.of(errors));
  }

  private Content render(
      Http.Request request,
      Locale locale,
      QuestionDefinition question,
      Optional<String> existingQuestionText,
      Optional<String> existingQuestionHelpText,
      Optional<String> errors) {

    String formAction =
        controllers.admin.routes.AdminQuestionTranslationsController.update(
                question.getId(), locale.toLanguageTag())
            .url();

    // Add form fields for questions.
    ImmutableList.Builder<FieldWithLabel> inputFields = ImmutableList.builder();
    inputFields.addAll(
        questionTextFields(
            question.getQuestionText().getDefault(),
            question.getQuestionHelpText().getDefault(),
            existingQuestionText,
            existingQuestionHelpText));
    inputFields.addAll(getQuestionTypeSpecificFields(question, locale));

    ContainerTag form =
        renderTranslationForm(request, locale, formAction, inputFields.build(), errors);

    return layout.render(
        renderHeader("Manage Question Translations"),
        renderLanguageLinks(question.getId(), locale),
        form);
  }

  @Override
  protected String languageLinkDestination(long questionId, Locale locale) {
    return controllers.admin.routes.AdminQuestionTranslationsController.edit(
            questionId, locale.toLanguageTag())
        .url();
  }

  private ImmutableList<FieldWithLabel> getQuestionTypeSpecificFields(
      QuestionDefinition question, Locale toUpdate) {
    switch (question.getQuestionType()) {
      case CHECKBOX:
      case DROPDOWN:
      case RADIO_BUTTON:
        MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) question;
        return multiOptionQuestionFields(multiOption.getOptions(), toUpdate);
      case ADDRESS:
      case ENUMERATOR:
      case FILEUPLOAD:
      case NAME:
      case NUMBER:
      case TEXT:
      default:
        return ImmutableList.of();
    }
  }

  private ImmutableList<FieldWithLabel> questionTextFields(
      String defaultText,
      String defaultHelpText,
      Optional<String> questionText,
      Optional<String> questionHelpText) {
    return ImmutableList.of(
        FieldWithLabel.input()
            .setId("localize-question-text")
            .setFieldName("questionText")
            .setLabelText(defaultText)
            .setPlaceholderText("Question text")
            .setValue(questionText),
        FieldWithLabel.input()
            .setId("localize-question-help-text")
            .setFieldName("questionHelpText")
            .setLabelText(defaultHelpText)
            .setPlaceholderText("Question help text")
            .setValue(questionHelpText));
  }

  private ImmutableList<FieldWithLabel> multiOptionQuestionFields(
      ImmutableList<QuestionOption> options, Locale toUpdate) {
    return options.stream()
        .map(
            option ->
                FieldWithLabel.input()
                    .setFieldName("options[]")
                    .setLabelText(option.optionText().getDefault())
                    .setPlaceholderText("Answer option")
                    .setValue(option.optionText().translations().getOrDefault(toUpdate, "")))
        .collect(toImmutableList());
  }
}
