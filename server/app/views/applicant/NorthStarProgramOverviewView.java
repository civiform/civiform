package views.applicant;

import auth.CiviFormProfile;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import java.util.Locale;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http;
import services.DeploymentType;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.program.ProgramDefinition;
import services.settings.SettingsManifest;
import views.NorthStarBaseView;

/**
 * Renders the program overview page for applicants, which describes the program to the applicant
 * before they start an application.
 */
public class NorthStarProgramOverviewView extends NorthStarBaseView {

  @Inject
  NorthStarProgramOverviewView(
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

  public String render(
      Messages messages,
      Http.Request request,
      Long applicantId,
      ApplicantPersonalInfo personalInfo,
      CiviFormProfile profile,
      ProgramDefinition programDefinition) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(
            request, Optional.of(applicantId), Optional.of(profile), personalInfo, messages);

    Locale preferredLocale = messages.lang().toLocale();

    context.setVariable(
        "pageTitle",
        messages.at(
            MessageKey.TITLE_PROGRAM_OVERVIEW.getKeyName(),
            programDefinition.localizedName().getOrDefault(preferredLocale)));

    String localizedProgramName = programDefinition.localizedName().getOrDefault(preferredLocale);
    context.setVariable("programName", localizedProgramName);

    String localizedProgramDescription = programDefinition.localizedDescription().getOrDefault(preferredLocale);
    context.setVariable("programDescription", localizedProgramDescription);

    String externalLink = programDefinition.externalLink();
    context.setVariable("externalLink", externalLink);

    return templateEngine.process("applicant/ProgramOverviewTemplate", context);
  }
}
