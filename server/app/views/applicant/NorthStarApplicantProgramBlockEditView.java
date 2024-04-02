package views.applicant;

import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.applicant.ApplicantRequestedAction;
import controllers.applicant.ApplicantRoutes;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http.Request;
import services.applicant.question.AddressQuestion;
import views.ApplicationBaseViewParams;
import views.html.helper.CSRF;

/** Renders a page for answering questions in a program screen (block). */
public final class NorthStarApplicantProgramBlockEditView extends NorthStarApplicantBaseView {

  @Inject
  NorthStarApplicantProgramBlockEditView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes) {
    super(templateEngine, playThymeleafContextFactory, assetsFinder, applicantRoutes);
  }

  public String render(Request request, ApplicationBaseViewParams applicationParams) {
    ThymeleafModule.PlayThymeleafContext context = createThymeleafContext(request);
    context.setVariable(
        "submitFormAction", getFormAction(applicationParams, ApplicantRequestedAction.NEXT_BLOCK));
    context.setVariable(
        "previousFormAction",
        getFormAction(applicationParams, ApplicantRequestedAction.PREVIOUS_BLOCK));
    context.setVariable(
        "reviewFormAction", getFormAction(applicationParams, ApplicantRequestedAction.REVIEW_PAGE));
    context.setVariable("csrfToken", CSRF.getToken(request.asScala()).value());
    context.setVariable("applicationParams", applicationParams);
    // TODO(#6910): Why am I unable to access static vars directly from Thymeleaf
    context.setVariable("stateAbbreviations", AddressQuestion.STATE_ABBREVIATIONS);
    return templateEngine.process("applicant/ApplicantProgramBlockEditTemplate", context);
  }

  private String getFormAction(
      ApplicationBaseViewParams params, ApplicantRequestedAction nextAction) {
    return applicantRoutes
        .updateBlock(
            params.profile(),
            params.applicantId(),
            params.programId(),
            params.block().getId(),
            params.inReview(),
            nextAction)
        .url();
  }
}
