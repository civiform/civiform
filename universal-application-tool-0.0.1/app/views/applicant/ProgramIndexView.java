package views.applicant;

import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.attributes.Attr.HREF;

import com.google.common.collect.ImmutableList;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
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
      Messages messages,
      long applicantId,
      ImmutableList<ProgramDefinition> programs,
      Optional<String> banner) {
    String applyMessage = messages.at("button.apply");
    ContainerTag body = body().withClasses("h-full w-full bg-orange-100 bg-opacity-30 overflow-x-auto absolute");
    if (banner.isPresent()) {
      // TODO: make this a styled toast.
      body.with(p(banner.get()));
    }
    body.with(branding(), status(), topContent(), mainContent(programs, applicantId));

    ContainerTag junkDiv = div().withClasses("hidden")
      .withText(applyMessage);
    body.with(junkDiv);

    return layout.render(body);
  }

  private ContainerTag branding() {
    return div()
      .withId("brand-id")
      .withClasses("absolute text-2xl top-8 left-8 text-red-400")
      .with(span("Civi"))
      .with(span("Form").withClasses("font-thin"));
  }

  private ContainerTag mainContent(ImmutableList<ProgramDefinition> programs, long applicantId) {
    return div().withId("main-content").withClasses("relative w-full px-8 flex flex-wrap pb-8").with(each(
                    programs,
                    program -> programCard(program, applicantId)));
  }

  private ContainerTag topContent() {
    ContainerTag floatTitle = 
      div().withId("float-title")
        .withText("get benefits")
        .withClasses("relative w-0 text-6xl font-serif font-thin");
    ContainerTag floatText = 
      div().withId("float-text")
        .withText("Civiform lets you apply for many benefits at once by reusing"
        + " information. You handle getting into it and we'll handle sending it"
        + " to the right places. Select an application to get started.")
        .withClasses("md:float-right md:absolute md:right-8 md:top-0 mt-4 md:ml-0 text-sm w-72 lg:w-96");
        
    return div()
      .withId("top-content")
      .withClasses("relative w-full h-auto mt-32 mb-16 px-8")
      .with(floatTitle, floatText);
  }

  private ContainerTag status() {
    return div()
      .withId("application-status")
      .withClasses("absolute top-8 right-8 text-sm underline")
      .with(span("view my applications"));
  }

  private ContainerTag programCard(ProgramDefinition program, Long applicantId) {
    String baseId = "program-card-" + program.id();
    ContainerTag category = div().withId(baseId + "-category").withClasses("text-xs pb-2").with(
      div().withClasses("h-3 w-3 bg-teal-400 rounded-full inline-block align-middle align-text-middle"),
      div("No Category").withClasses("ml-2 inline align-bottom align-text-bottom leading-3")
    );
    ContainerTag title = div().withId(baseId + "-title").withClasses("text-lg font-semibold").withText(program.name());
    ContainerTag description = div().withId(baseId + "-description").withClasses("text-xs my-2").withText(program.description());
    ContainerTag externalLink = div().withId(baseId + "-external-link").withClasses("text-xs underline").withText("Program details");
    ContainerTag programData = 
      div().withId(baseId + "-data").withClasses("px-4")
      .with(category, title, description, externalLink);

    String applyUrl = controllers.applicant.routes.ApplicantProgramsController.edit(applicantId, program.id()).url();     
    ContainerTag applyButton = a()
      .attr(HREF, applyUrl)
      .withText("Apply")
      .withId(baseId + "-apply")
      .withClasses("block uppercase rounded-3xl py-2 px-6 w-min mx-auto bg-gray-200 hover:bg-gray-300");

    ContainerTag applyDiv = div(applyButton).withClasses("absolute bottom-6 w-full");
    return div()
      .withId(baseId)
      .withClasses("relative inline-block mr-4 mb-4 application-card w-64 h-72 bg-white rounded-xl shadow-sm")
      .with(div().withClasses("h-3 rounded-t-xl bg-teal-400 bg-opacity-60 mb-4"))
      .with(programData)
      .with(applyDiv);
  }
}
