package views.admin.questions;

import com.google.common.collect.ImmutableList;
import j2html.tags.ContainerTag;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Langs;
import play.mvc.Http;
import play.twirl.api.Content;
import services.question.types.QuestionDefinition;
import views.admin.AdminLayout;
import views.admin.TranslationFormView;
import views.components.FieldWithLabel;

/** Renders a list of languages to select from, and a form for updating question information. */
public class QuestionTranslationView extends TranslationFormView {

  private final AdminLayout layout;

  @Inject
  public QuestionTranslationView(AdminLayout layout, Langs langs) {
    super(langs);
    this.layout = layout;
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
    ContainerTag form =
        renderTranslationForm(
            request,
            locale,
            formAction,
            formFields(
                question.getQuestionText().getDefault(),
                question.getQuestionHelpText().getDefault(),
                existingQuestionText,
                existingQuestionHelpText),
            errors);

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

  private ImmutableList<FieldWithLabel> formFields(
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
}
