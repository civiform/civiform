package views.applicant;

import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.applicant.ApplicantRequestedAction;
import controllers.applicant.ApplicantRoutes;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http.Request;
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
    context.setVariable("formAction", getFormAction(applicationParams));
    context.setVariable("csrfToken", CSRF.getToken(request.asScala()).value());
    context.setVariable("applicantQuestions", applicationParams.block().getQuestions());
    return templateEngine.process("applicant/ApplicantProgramBlockEditTemplate", context);
  }

  private String getFormAction(ApplicationBaseViewParams params) {
    return applicantRoutes
        .updateBlock(
            params.profile(),
            params.applicantId(),
            params.programId(),
            params.block().getId(),
            params.inReview(),
            ApplicantRequestedAction.NEXT_BLOCK)
        .url();
  }
}
