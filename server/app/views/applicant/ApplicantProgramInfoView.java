package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.H1Tag;
import java.util.Locale;
import java.util.Optional;
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
            .addMainStyles("mx-12", "my-8")
            .addMainContent(topContent(programTitle, programInfo, messages))
            .addMainContent(createButtons(applicantId, program.id(), messages));

    return layout.renderWithNav(request, userName, messages, bundle);
  }

  private DivTag topContent(String programTitle, String programInfo, Messages messages) {
    String programsLinkText = messages.at(MessageKey.TITLE_PROGRAMS.getKeyName());
    String homeLink = routes.HomeController.index().url();
    ATag allProgramsDiv =
        a().withHref(homeLink)
            .withClasses("text-gray-500", "text-left")
            .with(
                span("<").attr("aria-hidden", "true"),
                span().withText(programsLinkText).withClasses("px-4"));

    H1Tag titleDiv =
        h1().withText(programTitle)
            .withClasses(
                BaseStyles.TEXT_SEATTLE_BLUE, "text-2xl", "font-semibold", "text-gray-700", "mt-4");

    // "Markdown" the program description.
    ImmutableList<DomContent> items = TextFormatter.formatText(programInfo, false);

    DivTag descriptionDiv = div().withClasses("py-2").with(items);

    return div(allProgramsDiv, titleDiv, descriptionDiv);
  }

  private DivTag createButtons(Long applicantId, Long programId, Messages messages) {
    String applyUrl =
        controllers.applicant.routes.ApplicantProgramReviewController.preview(
                applicantId, programId)
            .url();
    ATag applyLink =
        a().withText(messages.at(MessageKey.BUTTON_APPLY.getKeyName()))
            .withHref(applyUrl)
            .withClasses(ReferenceClasses.APPLY_BUTTON, ApplicantStyles.BUTTON_PROGRAM_APPLY);
    DivTag buttonDiv =
        div(applyLink).withClasses("w-full", "mb-6", "flex-grow", "flex", "items-end");

    return buttonDiv;
  }
}
