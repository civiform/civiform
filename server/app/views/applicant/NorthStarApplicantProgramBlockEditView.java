package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.applicant.ApplicantRequestedAction;
import controllers.applicant.ApplicantRoutes;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http.Request;
import views.ApplicationBaseView;
import views.html.helper.CSRF;

/** Renders a page for answering questions in a program screen (block). */
public final class NorthStarApplicantProgramBlockEditView {
  private final TemplateEngine templateEngine;
  private final ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory;
  private final AssetsFinder assetsFinder;
  private final ApplicantRoutes applicantRoutes;

  @Inject
  NorthStarApplicantProgramBlockEditView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes) {
    this.templateEngine = checkNotNull(templateEngine);
    this.playThymeleafContextFactory = checkNotNull(playThymeleafContextFactory);
    this.assetsFinder = checkNotNull(assetsFinder);
    this.applicantRoutes = checkNotNull(applicantRoutes);
  }

  public String render(Request request, ApplicationBaseView.Params applicationParams) {
    ThymeleafModule.PlayThymeleafContext context = playThymeleafContextFactory.create(request);
    context.setVariable("tailwindStylesheet", assetsFinder.path("stylesheets/tailwind.css"));
    context.setVariable("uswdsStylesheet", assetsFinder.path("dist/uswds.min.css"));
    context.setVariable("uswdsJsBundle", assetsFinder.path("javascripts/uswds/uswds-init.min.js"));
    context.setVariable("adminJsBundle", assetsFinder.path("dist/admin.bundle.js"));
    context.setVariable("ApiDocsController", controllers.api.routes.ApiDocsController);
    context.setVariable("formAction", getFormAction(applicationParams));
    context.setVariable("csrfToken", CSRF.getToken(request.asScala()).value());
    return templateEngine.process("applicant/ApplicantProgramBlockEditView", context);
  }

  private String getFormAction(ApplicationBaseView.Params params) {
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
