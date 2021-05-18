package views.admin.questions;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import j2html.tags.ContainerTag;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Langs;
import play.mvc.Http;
import play.twirl.api.Content;
import services.LocalizedStrings;
import services.question.types.QuestionDefinition;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.TranslationFormView;
import views.components.FieldWithLabel;
import views.components.ToastMessage;

/** Renders a list of languages to select from, and a form for updating question information. */
public class QuestionTranslationView extends TranslationFormView {

  private final AdminLayout layout;

  @Inject
  public QuestionTranslationView(AdminLayout layout, Langs langs) {
    super(checkNotNull(langs));
    this.layout = checkNotNull(layout);
  }

  public Content render(Http.Request request, Locale locale, QuestionDefinition question) {
    return render(request, locale, question, Optional.empty());
  }

  public Content renderErrors(
      Http.Request request, Locale locale, QuestionDefinition invalidQuestion, String errors) {
    return render(request, locale, invalidQuestion, Optional.of(errors));
  }

  private Content render(
      Http.Request request, Locale locale, QuestionDefinition question, Optional<String> errors) {
    String formAction =
        controllers.admin.routes.AdminQuestionTranslationsController.update(
                question.getId(), locale.toLanguageTag())
            .url();
    ContainerTag form =
        renderTranslationForm(
            request,
            locale,
            formAction,
            formFields(locale, question.getQuestionText(), question.getQuestionHelpText()));

    String title = "Manage Question Translations";

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainContent(
                renderHeader(title), renderLanguageLinks(question.getId(), locale), form);
    errors.ifPresent(s -> htmlBundle.addToastMessages(ToastMessage.error(s).setDismissible(false)));

    return layout.renderCentered(htmlBundle);
  }

  @Override
  protected String languageLinkDestination(long questionId, Locale locale) {
    return controllers.admin.routes.AdminQuestionTranslationsController.edit(
            questionId, locale.toLanguageTag())
        .url();
  }

  private ImmutableList<FieldWithLabel> formFields(
      Locale locale, LocalizedStrings questionText, LocalizedStrings helpText) {
    ImmutableList.Builder<FieldWithLabel> fields = ImmutableList.builder();
    fields.add(
        FieldWithLabel.input()
            .setId("localize-question-text")
            .setFieldName("questionText")
            .setLabelText(questionText.getDefault())
            .setPlaceholderText("Question text")
            .setValue(questionText.maybeGet(locale)));

    // Help text is optional - only show if present.
    if (!helpText.isEmpty()) {
      fields.add(
          FieldWithLabel.input()
              .setId("localize-question-help-text")
              .setFieldName("questionHelpText")
              .setLabelText(helpText.getDefault())
              .setPlaceholderText("Question help text")
              .setValue(helpText.maybeGet(locale)));
    }

    return fields.build();
  }
}
