package views.admin.questions;

import com.google.common.collect.ImmutableList;
import j2html.tags.ContainerTag;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Langs;
import play.mvc.Http;
import play.twirl.api.Content;
import views.admin.AdminLayout;
import views.admin.TranslationFormView;
import views.components.FieldWithLabel;

public class QuestionTranslationView extends TranslationFormView {

  private final AdminLayout layout;

  @Inject
  public QuestionTranslationView(AdminLayout layout, Langs langs) {
    super(langs);
    this.layout = layout;
  }

  public Content render(
      Http.Request request,
      long questionId,
      Locale locale,
      Optional<String> existingQuestionText,
      Optional<String> existingQuestionHelpText,
      Optional<String> errors) {
    String formAction =
        controllers.admin.routes.AdminQuestionTranslationsController.update(
                questionId, locale.toLanguageTag())
            .url();
    ContainerTag form =
        renderTranslationForm(
            request,
            locale,
            formAction,
            formFields(existingQuestionText, existingQuestionHelpText),
            errors);

    return layout.render(
        renderHeader("Manage Question Translations"),
        renderLanguageLinks(questionId, locale),
        form);
  }

  @Override
  protected String languageLinkDestination(long questionId, Locale locale) {
    return controllers.admin.routes.AdminQuestionTranslationsController.edit(
            questionId, locale.toLanguageTag())
        .url();
  }

  private ImmutableList<FieldWithLabel> formFields(
      Optional<String> questionText, Optional<String> questionHelpText) {
    return ImmutableList.of(
        FieldWithLabel.input()
            .setId("localize-question-text")
            .setFieldName("questionText")
            .setPlaceholderText("Question text")
            .setValue(questionText),
        FieldWithLabel.input()
            .setId("localize-question-help-text")
            .setFieldName("questionHelpText")
            .setPlaceholderText("Question help text")
            .setValue(questionHelpText));
  }
}
