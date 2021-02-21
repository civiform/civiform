package views.admin.questions;

import static j2html.TagCreator.body;
import static j2html.TagCreator.div;

import com.google.inject.Inject;
import java.util.Optional;
import play.twirl.api.Content;
import services.question.QuestionDefinition;
import views.BaseHtmlLayout;
import views.BaseHtmlView;

public final class QuestionEditView extends BaseHtmlView {
  private final BaseHtmlLayout layout;

  @Inject
  public QuestionEditView(BaseHtmlLayout layout) {
    this.layout = layout;
  }

  public Content render(Optional<QuestionDefinition> question) {
    return layout.htmlContent(body(div("Not implemented yet")));
  }
}
