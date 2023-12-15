package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;

import auth.CiviFormProfile;
import controllers.applicant.ApplicantRoutes;
import controllers.applicant.routes;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantPersonalInfo.Representation;
import services.applicant.ReadOnlyApplicantProgramService;
import views.ApplicationBaseView;
import views.HtmlBundle;
import views.components.ButtonStyles;
import views.style.ApplicantStyles;
import views.style.StyleUtils;

/**
 * Renders a page indicating the applicant has not made any changes and asking them if they would
 * like to continue editing or exit the application.
 */
public final class PreventDuplicateSubmissionView extends ApplicationBaseView {

  private final ApplicantLayout layout;
  private final ApplicantRoutes applicantRoutes;

  @Inject
  PreventDuplicateSubmissionView(ApplicantLayout layout, ApplicantRoutes applicantRoutes) {
    this.layout = checkNotNull(layout);
    this.applicantRoutes = checkNotNull(applicantRoutes);
  }

  public Content render(
      Request request,
      ReadOnlyApplicantProgramService roApplicantProgramService,
      Messages messages,
      long applicantId,
      CiviFormProfile profile) {

    DivTag content =
        div()
            .withClasses(ApplicantStyles.PROGRAM_INFORMATION_BOX)
            .with(
                h2(messages.at(
                        MessageKey.TITLE_NO_CHANGES_TO_SAVE.getKeyName(),
                        roApplicantProgramService.getProgramTitle()))
                    .withClasses("text-black", "font-bold", "text-xl", "pb-8"))
            .with(p(messages.at(MessageKey.CONTENT_NO_CHANGES.getKeyName())).withClasses("pb-8"))
            .with(
                div()
                    .withClasses(
                        "flex", "flex-col", "gap-4", StyleUtils.responsiveSmall("flex-row"))
                    .with(
                        redirectButton(
                                "continue-editing-button",
                                messages.at(MessageKey.BUTTON_CONTINUE_EDITING.getKeyName()),
                                routes.ApplicantProgramReviewController.review(
                                        applicantId, roApplicantProgramService.getProgramId())
                                    .url())
                            .withClasses(ButtonStyles.SOLID_BLUE),
                        redirectButton(
                                "exit-application-button",
                                messages.at(MessageKey.BUTTON_EXIT_APPLICATION.getKeyName()),
                                applicantRoutes.index(profile, applicantId).url())
                            .withClasses(ButtonStyles.OUTLINED_TRANSPARENT)));

    String title = "No changes to save";
    HtmlBundle bundle =
        layout
            .getBundle(request)
            .setTitle(title)
            .addMainStyles(ApplicantStyles.MAIN_APPLICANT_INFO)
            .addMainContent(h1(title).withClasses("sr-only"), content);
    Optional<String> applicantName =
        roApplicantProgramService.getApplicantData().getApplicantName();

    return layout.renderWithNav(
        request,
        applicantName.isPresent()
            ? ApplicantPersonalInfo.ofLoggedInUser(
                Representation.builder().setName(applicantName).build())
            : ApplicantPersonalInfo.ofGuestUser(),
        messages,
        bundle,
        applicantId);
  }
}
