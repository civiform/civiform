package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.span;

import java.util.Locale;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import controllers.routes;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.TextFormatter;
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
      Optional<String> userName) {

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
    ImmutableList<DomContent> items = TextFormatter.formatText(programInfo, false);

    ContainerTag descriptionDiv = div().withClasses(Styles.PY_2).with(items);

    return div(allProgramsDiv, titleDiv, descriptionDiv);
  }

  private ContainerTag createButtons(Long applicantId, Long programId, Messages messages) {
    String applyUrl =
        controllers.applicant.routes.ApplicantProgramReviewController.preview(
                applicantId, programId)
            .url();
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
