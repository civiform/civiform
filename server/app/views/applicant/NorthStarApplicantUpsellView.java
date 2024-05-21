package views.applicant;

import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.DeploymentType;
import services.settings.SettingsManifest;

public class NorthStarApplicantUpsellView extends NorthStarApplicantBaseView {

  @Inject
  NorthStarApplicantUpsellView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes,
      SettingsManifest settingsManifest,
      LanguageUtils languageUtils,
      DeploymentType deploymentType) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        assetsFinder,
        applicantRoutes,
        settingsManifest,
        languageUtils,
        deploymentType);
  }

  public String render(UpsellParams params) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(
            params.request(),
            params.applicantId(),
            params.profile(),
            params.applicantPersonalInfo(),
            params.messages());

    context.setVariable("programName", params.programTitle().orElse(""));
    context.setVariable("applicationId", params.applicationId());

    return templateEngine.process("applicant/ApplicantUpsellTemplate", context);
  }
}