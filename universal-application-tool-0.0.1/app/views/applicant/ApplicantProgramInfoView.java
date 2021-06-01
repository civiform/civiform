package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.li;
import static j2html.TagCreator.span;
import static j2html.TagCreator.ul;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import controllers.routes;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import java.util.ArrayList;
import java.util.Locale;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.Accordion;
import views.style.ApplicantStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.Styles;

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
            .addMainStyles(Styles.MX_12, Styles.MY_8)
            .addMainContent(topContent(programTitle, programInfo, messages))
            .addMainContent(createButtons(applicantId, program.id(), messages));

    return layout.renderWithNav(request, userName, messages, bundle);
  }

  private ContainerTag topContent(String programTitle, String programInfo, Messages messages) {
    String programsLinkText = messages.at(MessageKey.TITLE_PROGRAMS.getKeyName());
    String homeLink = routes.HomeController.index().url();
    ContainerTag allProgramsDiv =
        a("<")
            .withHref(homeLink)
            .withClasses(Styles.TEXT_GRAY_500, Styles.TEXT_LEFT)
            .with(span().withText(programsLinkText).withClasses(Styles.PX_4));

    ContainerTag titleDiv =
        h2().withText(programTitle)
            .withClasses(
                BaseStyles.TEXT_SEATTLE_BLUE,
                Styles.TEXT_2XL,
                Styles.FONT_SEMIBOLD,
                Styles.TEXT_GRAY_700,
                Styles.MT_4);

    // "Markdown" the program description.
    String[] lines = Iterables.toArray(Splitter.on("\n").split(programInfo), String.class);
    ImmutableList<DomContent> items = formatText(lines, false);
    ContainerTag descriptionDiv = div().withClasses(Styles.PY_2).with(items);

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
        builder.add(div().withClasses(Styles.H_6));
      }
    }
    return builder.build();
  }

  private ContainerTag buildAccordion(String title, ArrayList<String> accordionContent) {
    Accordion accordion = new Accordion().setTitle(title);
    ImmutableList<DomContent> contentTags =
        formatText(accordionContent.toArray(new String[0]), true);
    contentTags.stream().forEach(tag -> accordion.addContent(tag));
    return accordion.getContainer();
  }

  private ContainerTag buildList(ArrayList<String> items) {
    ContainerTag listTag = ul().withClasses(Styles.LIST_DISC, Styles.MX_8);
    items.forEach(item -> listTag.with(li().withText(item)));
    return listTag;
  }

  private ContainerTag createButtons(Long applicantId, Long programId, Messages messages) {
    String applyUrl =
        controllers.applicant.routes.ApplicantProgramsController.edit(applicantId, programId).url();
    ContainerTag applyLink =
        a().withText(messages.at(MessageKey.BUTTON_APPLY.getKeyName()))
            .withHref(applyUrl)
            .withClasses(ReferenceClasses.APPLY_BUTTON, ApplicantStyles.BUTTON_PROGRAM_APPLY);
    ContainerTag buttonDiv =
        div(applyLink)
            .withClasses(
                Styles.W_FULL, Styles.MB_6, Styles.FLEX_GROW, Styles.FLEX, Styles.ITEMS_END);

    return buttonDiv;
  }
}
