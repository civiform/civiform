package views.admin;

import static j2html.attributes.Attr.FORMACTION;
import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;
import static j2html.TagCreator.button;
import static j2html.TagCreator.form;
import static j2html.TagCreator.each;

import com.google.common.collect.ImmutableList;
import javax.inject.Inject;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.ViewUtils;

public final class ProgramList extends BaseHtmlView {

  public Content render(ImmutableList<ProgramDefinition> programs) {
    return htmlContent(
        body(
            h1("Programs"),
            div(form(button("Add a Program").attr(FORMACTION, "/admin/programs/new"))),
            div(each(programs, program -> div(h2(program.name()), p(program.description()))))));
  }
}
