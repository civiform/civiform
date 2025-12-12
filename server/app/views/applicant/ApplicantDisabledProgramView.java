package views.applicant;

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
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.settings.SettingsManifest;
import views.NorthStarBaseView;

/** renders a info page for applicants trying to access a disabled program via its deep link */
public final class ApplicantDisabledProgramView extends NorthStarBaseView {

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

    String title = messages.at(MessageKey.TITLE_PROGRAM_NOT_AVAILABLE.getKeyName());
    String subtitle = messages.at(MessageKey.CONTENT_DISABLED_PROGRAM_INFO.getKeyName());
    String buttonText = messages.at(MessageKey.BUTTON_HOME_PAGE.getKeyName());
    String homeUrl = routes.HomeController.index().url();

    context.setVariable("title", title);
    context.setVariable("subtitle", subtitle);
    context.setVariable("additionalInfo", "");
    context.setVariable("buttonText", buttonText);
    context.setVariable("homeUrl", homeUrl);
    context.setVariable("statusCode", "");
    context.setVariable("pageTitle", title);

    return templateEngine.process("applicant/ApplicantDisabledProgramTemplate.html", context);
  }
}
