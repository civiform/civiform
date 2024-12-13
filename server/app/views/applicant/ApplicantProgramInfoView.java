package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.span;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.applicant.ApplicantRoutes;
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
import services.applicant.ApplicantPersonalInfo;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.ButtonStyles;
import views.components.TextFormatter;
import views.style.BaseStyles;
import views.style.ReferenceClasses;

/** Shows information for a specific program with a button to start the application. */
public class ApplicantProgramInfoView extends BaseHtmlView {

  private final ApplicantLayout layout;
  private final ApplicantRoutes applicantRoutes;

  @Inject
  public ApplicantProgramInfoView(ApplicantLayout layout, ApplicantRoutes applicantRoutes) {
    this.layout = checkNotNull(layout);
    this.applicantRoutes = checkNotNull(applicantRoutes);
  }

  public Content render(
      Messages messages,
      ProgramDefinition program,
      Http.Request request,
      long applicantId,
      ApplicantPersonalInfo personalInfo,
      CiviFormProfile profile) {

    Locale preferredLocale = messages.lang().toLocale();
    String programTitle = program.localizedName().getOrDefault(preferredLocale);
    String programInfo = program.localizedDescription().getOrDefault(preferredLocale);

    HtmlBundle bundle =
        layout
            .getBundle(request)
            .addMainStyles("mx-12", "my-8")
            .addMainContent(topContent(programTitle, programInfo, messages))
            .addMainContent(createButtons(applicantId, program.id(), messages, profile));

    return layout.renderWithNav(request, personalInfo, messages, bundle, Optional.of(applicantId));
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
                BaseStyles.TEXT_CIVIFORM_BLUE,
                "text-2xl",
                "font-semibold",
                "text-gray-700",
                "mt-4");

    // "Markdown" the program description.
    ImmutableList<DomContent> items =
        TextFormatter.formatTextWithAriaLabel(
            programInfo,
            /* preserveEmptyLines= */ true,
            /* addRequiredIndicator= */ false,
            messages.at(MessageKey.LINK_OPENS_NEW_TAB_SR.getKeyName()).toLowerCase(Locale.ROOT));
    DivTag descriptionDiv = div().withClasses("py-2").with(items);

    return div(allProgramsDiv, titleDiv, descriptionDiv);
  }

  private DivTag createButtons(
      Long applicantId, Long programId, Messages messages, CiviFormProfile profile) {
    String applyUrl = applicantRoutes.review(profile, applicantId, programId).url();
    ATag applyLink =
        a().withText(messages.at(MessageKey.BUTTON_APPLY.getKeyName()))
            .withHref(applyUrl)
            .withClasses(ReferenceClasses.APPLY_BUTTON, ButtonStyles.SOLID_BLUE_TEXT_SM, "mx-auto");
    DivTag buttonDiv =
        div(applyLink).withClasses("w-full", "mb-6", "flex-grow", "flex", "items-end");

    return buttonDiv;
  }
}
