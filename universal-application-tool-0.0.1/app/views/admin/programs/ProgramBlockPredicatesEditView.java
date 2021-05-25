package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;

import com.google.common.collect.ImmutableList;
import j2html.tags.Tag;
import javax.inject.Inject;
import play.mvc.Http;
import play.twirl.api.Content;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;

public class ProgramBlockPredicatesEditView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public ProgramBlockPredicatesEditView(AdminLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(
      Http.Request request,
      ProgramDefinition programDefinition,
      BlockDefinition blockDefinition,
      ImmutableList<QuestionDefinition> potentialPredicateQuestions) {
    String title = "Edit block predicates";
    Tag csrfTag = makeCsrfTokenInputTag(request);
    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainContent(
                form().with(csrfTag),
                h2().withText(programDefinition.adminName()),
                h1().withText(
                        String.format("Add show/hide predicates for %s", blockDefinition.name())),
                div()
                    .with(
                        each(
                            potentialPredicateQuestions,
                            question -> div().withText(question.getName()))));

    return layout.renderCentered(htmlBundle);
  }
}
