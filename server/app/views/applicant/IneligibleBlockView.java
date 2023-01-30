package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.*;

import controllers.applicant.routes;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.UlTag;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.ReadOnlyApplicantProgramService;
import views.ApplicationBaseView;
import views.HtmlBundle;
import views.components.LinkElement;
import views.style.ApplicantStyles;
import views.style.StyleUtils;

/** Renders a page indicating the applicant is not eligible for a program. */
public final class IneligibleBlockView extends ApplicationBaseView {

  private final ApplicantLayout layout;

  @Inject
  IneligibleBlockView(ApplicantLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(
      Request request,
      ReadOnlyApplicantProgramService roApplicantProgramService,
      Optional<String> applicantName,
      Messages messages,
      long applicantId,
      long programId) {
    // TODO(#3744): Translate these strings.
    ATag infoLink =
        new LinkElement()
            .setStyles("mb-4", "underline")
            .setText(messages.at("program details"))
            .setHref(routes.ApplicantProgramsController.view(applicantId, programId).url())
            .asAnchorText()
            .attr("aria-label", "program details");
    UlTag listTag = ul().withClasses("list-disc", "mx-8");
    roApplicantProgramService
        .getEligibilityQuestionsForProgram()
        .forEach(question -> listTag.with(li().withText(question.getQuestionText())));

    DivTag content =
        div()
            .withClasses(ApplicantStyles.PROGRAM_INFORMATION_BOX)
            .with(
                h2(String.format(
                        "Based on your responses, you may not qualify for the %s",
                        roApplicantProgramService.getProgramTitle()))
                    .withClasses("mb-4"))
            .with(div(messages.at("You must meet these program requirements:")).withClasses("mb-4"))
            .with(div().with(listTag).withClasses("mb-4"))
            .with(
                div(messages.at("For eligibility criteria please refer to "))
                    .with(infoLink)
                    .withClasses("mb-4"))
            .with(
                div(messages.at(
                        "You can return to the previous page to edit your answers. Or apply to"
                            + " another program."))
                    .withClasses("mb-4"))
            .with(
                div()
                    .withClasses(
                        "flex", "flex-col", "gap-4", StyleUtils.responsiveSmall("flex-row"))
                    // Empty div to push buttons to the right on desktop.
                    .with(div().withClasses("flex-grow"))
                    .with(
                        new LinkElement()
                            .setHref(routes.ApplicantProgramsController.index(applicantId).url())
                            .setText(
                                messages.at(MessageKey.LINK_APPLY_TO_ANOTHER_PROGRAM.getKeyName()))
                            .asButton()
                            .withClasses(ApplicantStyles.BUTTON_NOT_RIGHT_NOW))
                    .with(
                        new LinkElement()
                            .setHref(
                                routes.ApplicantProgramReviewController.review(
                                        applicantId, programId)
                                    .url())
                            .setText(messages.at("Go back and edit"))
                            .asButton()
                            .withClasses(ApplicantStyles.BUTTON_CREATE_ACCOUNT)));
    String title = "Ineligible for program";
    HtmlBundle bundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainStyles(ApplicantStyles.MAIN_APPLICANT_INFO)
            .addMainContent(h1(title).withClasses("sr-only"), content);

    return layout.renderWithNav(request, applicantName, messages, bundle);
  }
}
