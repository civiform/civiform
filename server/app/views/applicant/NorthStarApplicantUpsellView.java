package views.applicant;

import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import controllers.applicant.routes;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.DeploymentType;
import services.settings.SettingsManifest;
import views.NorthStarBaseView;

public class NorthStarApplicantUpsellView extends NorthStarBaseView {

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
    context.setVariable("bannerMessage", params.bannerMessage());

    // Info for login modal
    String applyToProgramsUrl = applicantRoutes.index(params.profile(), params.applicantId()).url();
    context.setVariable("upsellBypassUrl", applyToProgramsUrl);
    context.setVariable(
        "upsellLoginUrl",
        controllers.routes.LoginController.applicantLogin(Optional.of(applyToProgramsUrl)).url());

    String downloadHref =
        routes.UpsellController.download(params.applicationId(), params.applicantId()).url();
    context.setVariable("downloadHref", downloadHref);

    return templateEngine.process("applicant/ApplicantUpsellTemplate", context);
  }
}
