package views.admin.questions;

import java.util.Locale;
import javax.inject.Inject;
import play.i18n.Langs;
import play.twirl.api.Content;
import views.TranslationFormView;
import views.admin.AdminLayout;

public class QuestionTranslationView extends TranslationFormView {

  private final AdminLayout layout;

  @Inject
  public QuestionTranslationView(AdminLayout layout, Langs langs) {
    super(langs);
    this.layout = layout;
  }

  public Content render(long questionId, Locale locale) {
    return layout.render(
        renderHeader("Manage Question Translations"), renderLanguageLinks(questionId, locale));
  }

  @Override
  protected String languageLinkDestination(long questionId, Locale locale) {
    return controllers.admin.routes.AdminQuestionTranslationsController.edit(
            questionId, locale.toLanguageTag())
        .url();
  }
}
