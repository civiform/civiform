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
import play.i18n.Messages;
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
   * @param messages the localized {@link Messages} for the current applicant
   * @param applicantId the ID of the current applicant
   * @param programs an {@link ImmutableList} of {@link ProgramDefinition}s with the most recent
   *     published versions
   * @return HTML content for rendering the list of available programs
   */
  public Content render(
          Messages messages, long applicantId, ImmutableList<ProgramDefinition> programs) {
    String applyMessage = messages.at("apply");
    return layout.render(
        body()
            .with(h1(messages.at("programs")))
            .with(
                each(
                    programs,
                    program -> shortProgram(applicantId, applyMessage, program))));
  }

  private Tag shortProgram(
      long applicantId, String applyMessage, ProgramDefinition program) {
    return div()
        .with(h2(program.name()))
        .with(
            a(applyMessage)
                .withId(String.format("apply%d", program.id()))
                .attr(
                    HREF,
                    controllers.applicant.routes.ApplicantProgramsController.edit(
                            applicantId, program.id())
                        .url()));
  }
}
