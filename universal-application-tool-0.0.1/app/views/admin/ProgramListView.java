package views.admin;

import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;
import static j2html.attributes.Attr.FORMACTION;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlLayout;
import views.BaseHtmlView;

public final class ProgramListView extends BaseHtmlView {
  private final BaseHtmlLayout layout;

  @Inject
  public ProgramListView(BaseHtmlLayout layout) {
    this.layout = layout;
  }

  public Content render(ImmutableList<ProgramDefinition> programs) {
    return layout.htmlContent(
        body(
            h1("Programs"),
            div(
                form(
                    button("Add a Program")
                        .attr(
                            FORMACTION, controllers.admin.routes.AdminProgramController.newOne()))),
            div(each(programs, program -> div(h2(program.name()), p(program.description()))))));
  }
}
