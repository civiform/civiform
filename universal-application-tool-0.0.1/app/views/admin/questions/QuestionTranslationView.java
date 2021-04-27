package views.admin.questions;

import static j2html.TagCreator.div;
import static j2html.TagCreator.p;

import javax.inject.Inject;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.admin.AdminLayout;

public class QuestionTranslationView extends BaseHtmlView {

  private final AdminLayout layout;

  @Inject
  public QuestionTranslationView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content render(String questionText) {
    return layout.render(
        renderHeader("Manage Question Translations"),
        div().with(p("Question text: " + questionText)));
  }
}
