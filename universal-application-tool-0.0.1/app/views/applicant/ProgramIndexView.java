package views.applicant;

import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.attributes.Attr.HREF;

import com.google.common.collect.ImmutableList;
import j2html.tags.Tag;
import javax.inject.Inject;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlView;

/** Returns a list of programs that an applicant can browse, with buttons for applying. */
public class ProgramIndexView extends BaseHtmlView {

  private final ApplicantLayout layout;

  @Inject
  public ProgramIndexView(ApplicantLayout layout) {
    this.layout = layout;
  }

  /**
   * For each program in the list, render the program information along with an "Apply" button that
   * redirects the user to that program's application.
   *
   * @param programs an {@link ImmutableList} of {@link ProgramDefinition}s with the most recent
   *     published versions
   * @return HTML content for rendering the list of available programs
   */
  public Content render(long applicantId, ImmutableList<ProgramDefinition> programs) {
    return layout.render(
        body()
            .with(h1("Programs"))
            .with(each(programs, program -> shortProgram(applicantId, program))));
  }

  private Tag shortProgram(long applicantId, ProgramDefinition program) {
    return div()
        .with(h2(program.name()))
        .with(
            a("Apply")
                .withId(String.format("apply%d", program.id()))
                .attr(
                    HREF,
                    controllers.applicant.routes.ApplicantProgramsController.edit(
                            applicantId, program.id())
                        .url()));
  }
}
