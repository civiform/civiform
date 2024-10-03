package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.li;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.ul;

import auth.CiviFormProfile;
import controllers.applicant.ApplicantRoutes;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.UlTag;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.program.ProgramDefinition;
import views.ApplicationBaseView;
import views.HtmlBundle;
import views.components.ButtonStyles;
import views.components.Icons;
import views.components.LinkElement;
import views.components.TextFormatter;
import views.style.ApplicantStyles;
import views.style.StyleUtils;

/** Renders a page indicating the applicant is not eligible for a program. */
public final class IneligibleBlockView extends ApplicationBaseView {

  private final ApplicantLayout layout;
  private final ApplicantRoutes applicantRoutes;
  private final ApplicantService applicantService;

  @Inject
  IneligibleBlockView(
      ApplicantLayout layout, ApplicantRoutes applicantRoutes, ApplicantService applicantService) {
    this.layout = checkNotNull(layout);
    this.applicantRoutes = checkNotNull(applicantRoutes);
    this.applicantService = checkNotNull(applicantService);
  }

  public Content render(
      Request request,
      CiviFormProfile submittingProfile,
      ReadOnlyApplicantProgramService roApplicantProgramService,
      Messages messages,
      long applicantId,
      ProgramDefinition programDefinition) {
    long programId = roApplicantProgramService.getProgramId();
    boolean isTrustedIntermediary = submittingProfile.isTrustedIntermediary();
    String programDetailsLink = programDefinition.externalLink();
    ATag infoLink = null;
    if (!programDetailsLink.isEmpty()) {
      infoLink =
          new LinkElement()
              .setStyles("mb-4", "underline")
              .setText(
                  messages
                      .at(MessageKey.LINK_PROGRAM_DETAILS.getKeyName())
                      .toLowerCase(Locale.ROOT))
              .setHref(programDetailsLink)
              .opensInNewTab()
              .setIcon(Icons.OPEN_IN_NEW, LinkElement.IconPosition.END)
              .asAnchorText()
              .attr(
                  "aria-label",
                  messages
                      .at(MessageKey.LINK_PROGRAM_DETAILS_SR.getKeyName())
                      .toLowerCase(Locale.ROOT));
    }
    UlTag listTag = ul().withClasses("list-disc", "mx-8");
    roApplicantProgramService
        .getIneligibleQuestions()
        .forEach(
            question ->
                listTag.with(
                    li().with(
                            div()
                                .with(
                                    TextFormatter.formatTextWithAriaLabel(
                                        question.getQuestionText(), /* preserveEmptyLines */
                                        true, /* addRequiredIndicator */
                                        false,
                                        messages
                                            .at(MessageKey.LINK_OPENS_NEW_TAB_SR.getKeyName())
                                            .toLowerCase(Locale.ROOT))))));

    DivTag content =
        div()
            .withClasses(ApplicantStyles.PROGRAM_INFORMATION_BOX)
            .with(
                h2(messages.at(
                        isTrustedIntermediary
                            ? MessageKey.TITLE_APPLICATION_NOT_ELIGIBLE_TI.getKeyName()
                            : MessageKey.TITLE_APPLICATION_NOT_ELIGIBLE.getKeyName(),
                        roApplicantProgramService.getProgramTitle()))
                    .withClasses("mb-4"))
            .with(div().with(listTag).withClasses("mb-4"))
            .condWith(
                infoLink != null,
                div(rawHtml(
                        messages.at(
                            MessageKey.CONTENT_ELIGIBILITY_CRITERIA.getKeyName(), infoLink)))
                    .withClasses("mb-4"))
            .with(
                div(messages.at(MessageKey.CONTENT_CHANGE_ELIGIBILITY_ANSWERS.getKeyName()))
                    .withClasses("mb-4"))
            .with(
                div()
                    .withClasses(
                        "flex", "flex-col", "gap-4", StyleUtils.responsiveSmall("flex-row"))
                    // Empty div to push buttons to the right on desktop.
                    .with(div().withClasses("flex-grow"))
                    .with(
                        new LinkElement()
                            .setHref(applicantRoutes.index(submittingProfile, applicantId).url())
                            .setText(
                                messages.at(MessageKey.LINK_APPLY_TO_ANOTHER_PROGRAM.getKeyName()))
                            .asButton()
                            .withClasses(ButtonStyles.OUTLINED_TRANSPARENT))
                    .with(
                        new LinkElement()
                            .setHref(
                                applicantRoutes
                                    .review(submittingProfile, applicantId, programId)
                                    .url())
                            .setText(messages.at(MessageKey.BUTTON_GO_BACK_AND_EDIT.getKeyName()))
                            .asButton()
                            .withClasses(ButtonStyles.SOLID_BLUE)));
    String title = "Ineligible for program";
    HtmlBundle bundle =
        layout
            .getBundle(request)
            .setTitle(title)
            .addMainStyles(ApplicantStyles.MAIN_APPLICANT_INFO)
            .addMainContent(h1(title).withClasses("sr-only"), content);

    return layout.renderWithNav(
        request,
        applicantService.getPersonalInfo(applicantId).toCompletableFuture().join(),
        messages,
        bundle,
        Optional.of(applicantId));
  }
}
