package views.applicant.disabled;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import controllers.LanguageUtils;
import controllers.routes;
import java.util.Optional;
import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http;
import services.BundledAssetsFinder;
import services.DeploymentType;
import services.applicant.ApplicantPersonalInfo;
import services.settings.SettingsManifest;
import views.applicant.ApplicantBaseView;

/** renders a info page for applicants trying to access a disabled program via its deep link */
public final class ApplicantDisabledProgramView extends ApplicantBaseView {

  private final ProfileUtils profileUtils;

  @Inject
  public ApplicantDisabledProgramView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      BundledAssetsFinder bundledAssetsFinder,
      controllers.applicant.ApplicantRoutes applicantRoutes,
      SettingsManifest settingsManifest,
      LanguageUtils languageUtils,
      DeploymentType deploymentType,
      ProfileUtils profileUtils) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        bundledAssetsFinder,
        applicantRoutes,
        settingsManifest,
        languageUtils,
        deploymentType);
    this.profileUtils = checkNotNull(profileUtils);
  }

  public String render(
      Messages messages,
      Http.Request request,
      long applicantId,
      ApplicantPersonalInfo personalInfo) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(
            request,
            Optional.of(applicantId),
            Optional.of(profileUtils.currentUserProfile(request)),
            personalInfo,
            messages);

    context.setVariable("homeUrl", routes.HomeController.index().url());

    return templateEngine.process("applicant/disabled/ApplicantDisabledProgramTemplate", context);
  }
}
