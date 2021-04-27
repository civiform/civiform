package views.admin.questions;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import javax.inject.Inject;
import play.i18n.Lang;
import play.i18n.Langs;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.admin.AdminLayout;

public class QuestionTranslationView extends BaseHtmlView {

  private final AdminLayout layout;
  private final ImmutableList<Locale> supportedLanguages;

  @Inject
  public QuestionTranslationView(AdminLayout layout, Langs langs) {
    this.layout = layout;
    this.supportedLanguages =
        langs.availables().stream().map(Lang::toLocale).collect(toImmutableList());
  }

  public Content render() {
    return layout.render();
  }
}
