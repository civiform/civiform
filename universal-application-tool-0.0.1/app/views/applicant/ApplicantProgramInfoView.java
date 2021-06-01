package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.li;
import static j2html.TagCreator.span;
import static j2html.TagCreator.ul;
import static j2html.attributes.Attr.HREF;

import com.google.common.collect.Iterables;
import controllers.routes;
import com.google.common.base.Splitter;
import com.google.inject.Inject;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import com.google.common.collect.ImmutableList;
import play.i18n.Messages;
import play.mvc.Http;
import java.util.Locale;
import java.util.ArrayList;
import j2html.tags.DomContent;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.Accordion;

/** Shows information for a specific program with a button to start the application. */
public class ApplicantProgramInfoView extends BaseHtmlView {

  private final ApplicantLayout layout;

  @Inject
  public ApplicantProgramInfoView(ApplicantLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(
      Messages messages,
      ProgramDefinition program,
      Http.Request request,
      long applicantId,
      String userName) {
          
    Locale preferredLocale = messages.lang().toLocale();
    String programTitle = program.localizedName().getOrDefault(preferredLocale);
    String programInfo = program.localizedDescription().getOrDefault(preferredLocale);
   

    HtmlBundle bundle =
        layout
            .getBundle()
            .addMainStyles("mx-12 my-8")
            .addMainContent(topContent(programTitle, programInfo))
            .addMainContent(createButtons(applicantId, program.id()));

    return layout.renderWithNav(request, userName, messages, bundle);
  }

  private ContainerTag topContent(String programTitle, String programInfo) {
    String homeLink = routes.HomeController.index().url();
    ContainerTag allProgramsDiv = a("<").withHref(homeLink).withClasses("text-gray-500 text-left")
        .with(span().withText("Programs & services").withClasses("px-4"));

    ContainerTag titleDiv =
        h2().withText(programTitle).withClasses("text-seattle-blue text-2xl font-semibold text-gray-700 mt-4");
    
    // "Markdown" the program description.
    String[] lines = Iterables.toArray(Splitter.on("\n").split(programInfo), String.class);
    ImmutableList<DomContent> items = formatText(lines, false);
    ContainerTag descriptionDiv = div().withClasses("py-2").with(items);

    return div(allProgramsDiv, titleDiv, descriptionDiv);
  }

  /** Adds the ability to create accordions and lists from data in text fields. */
  private ImmutableList<DomContent> formatText(String[] lines, boolean preserveEmptyLines) {
    ImmutableList.Builder<DomContent> builder = new ImmutableList.Builder<DomContent>();
    for (int i = 0; i < lines.length; i++) {
        String line = lines[i].trim();
        if (line.startsWith("###")) { // We're calling this an accordion.
            String accordionHeader = line.substring(3);
            int next = i + 1;
            ArrayList<String> content = new ArrayList<>();
            while (next < lines.length && lines[next].startsWith(">")) {
                content.add(lines[next].substring(1));
                next++;
            }
            i = next - 1;
            builder.add(buildAccordion(accordionHeader, content));
        } else if (line.startsWith("*")) { // unordered list item.
            ArrayList<String> items = new ArrayList<>();
            items.add(line.substring(1));
            int next = i + 1;
            while (next < lines.length && lines[next].startsWith("*")) {
                items.add(lines[next].substring(1));
                next++;
            }
            i = next - 1;
            builder.add(buildList(items));
        } else if (line.length() > 0) {
            ImmutableList<DomContent> lineContent = createLinksAndEscapeText(line);
            builder.add(div().with(lineContent));
        } else if (preserveEmptyLines) {
            builder.add(div().withClasses("h-6"));
        }
    }
    return builder.build();
  }

  private ContainerTag buildAccordion(String title, ArrayList<String> accordionContent) {
    Accordion accordion = new Accordion().setTitle(title);
    ImmutableList<DomContent> contentTags = formatText(accordionContent.toArray(new String[0]), true);
    contentTags.stream().forEach(tag -> accordion.addContent(tag));
    return accordion.getContainer();
  }

  private ContainerTag buildList(ArrayList<String> items) {
      ContainerTag listTag = ul().withClasses("list-disc mx-8");
      items.forEach(item -> listTag.with(li().withText(item)));
      return listTag;
  }

  private ContainerTag createButtons(Long applicantId, Long programId) {
    String applyUrl =
        controllers.applicant.routes.ApplicantProgramsController.edit(applicantId, programId).url();
    ContainerTag applyLink =
        a("Apply")
            .withClasses(
                "cf-apply-button block py-2 text-center rounded-full bg-seattle-blue text-white"
                    + " rounded-full hover:bg-blue-700 disabled:bg-gray-200 disabled:text-gray-400"
                    + " uppercase font-semibold px-8 text-sm mx-auto")
            .attr(HREF, applyUrl);
     ContainerTag buttonDiv = div(applyLink).withClasses("w-full mb-6 flex-grow flex items-end");

    return buttonDiv;
  }
}
