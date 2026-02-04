package views.errors;

import com.google.inject.Inject;
import controllers.LanguageUtils;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http;
import services.BundledAssetsFinder;
import services.DeploymentType;
import services.applicant.ApplicantPersonalInfo;
import services.settings.SettingsManifest;
import views.applicant.ApplicantBaseView;

/**
 * Renders a page to handle internal server errors that will be shown to users instead of the
 * unthemed default Play page.
 */
public final class InternalServerError extends ApplicantBaseView {

  @Inject
  public InternalServerError(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      BundledAssetsFinder bundledAssetsFinder,
      controllers.applicant.ApplicantRoutes applicantRoutes,
      SettingsManifest settingsManifest,
      LanguageUtils languageUtils,
      DeploymentType deploymentType) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        bundledAssetsFinder,
        applicantRoutes,
        settingsManifest,
        languageUtils,
        deploymentType);
  }

  public String render(Http.Request request, Messages messages, String exceptionId) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(
            request,
            Optional.empty(),
            Optional.empty(),
            ApplicantPersonalInfo.ofGuestUser(),
            messages);

    String supportEmail = settingsManifest.getSupportEmailAddress(request).orElse("");

    context.setVariable("exceptionId", exceptionId);
    context.setVariable("supportEmail", supportEmail);
    context.setVariable("homeUrl", controllers.routes.HomeController.index().url());

    return templateEngine.process("errors/InternalServerErrorTemplate.html", context);
  }
}
