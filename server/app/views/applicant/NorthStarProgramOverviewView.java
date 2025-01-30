package views.applicant;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import services.AlertSettings;
import services.AlertType;
import services.DeploymentType;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.program.ProgramDefinition;
import services.settings.SettingsManifest;
import views.NorthStarBaseView;
import views.components.TextFormatter;

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

    String localizedProgramDescription = getProgramDescription(programDefinition, preferredLocale);
    context.setVariable("programDescription", localizedProgramDescription);

    ImmutableMap<String, String> applicationStepsMap =
        getStepsMap(programDefinition, preferredLocale);
    context.setVariable("applicationSteps", applicationStepsMap.entrySet());

    AlertSettings eligibilityAlertSettings = createEligibilityAlertSettings(messages);
    context.setVariable("eligibilityAlertSettings", eligibilityAlertSettings);

    context.setVariable("createAccountLink", controllers.routes.LoginController.register().url());

    // This works for logged-in and logged-out applicants
    String actionUrl = applicantRoutes.edit(profile, applicantId, programDefinition.id()).url();
    context.setVariable("actionUrl", actionUrl);

    return templateEngine.process("applicant/ProgramOverviewTemplate", context);
  }

  private String getProgramDescription(
      ProgramDefinition programDefinition, Locale preferredLocale) {
    String localizedProgramDescription =
        programDefinition.localizedDescription().getOrDefault(preferredLocale);

    if (!localizedProgramDescription.isEmpty()) {
      return TextFormatter.formatTextToSanitizedHTML(
          localizedProgramDescription,
          /* preserveEmptyLines= */ true,
          /* addRequiredIndicator= */ false);
    }

    return programDefinition.localizedShortDescription().getOrDefault(preferredLocale);
  }

  private AlertSettings createEligibilityAlertSettings(Messages messages) {
    String alertText = messages.at(MessageKey.ALERT_LIKELY_ELIGIBLE.getKeyName());
    AlertSettings eligibilityAlertSettings =
        new AlertSettings(
            /* show= */ true,
            Optional.empty(),
            alertText,
            AlertType.INFO,
            ImmutableList.of(),
            /* isSlim= */ true);
    return eligibilityAlertSettings;
  }

  private ImmutableMap<String, String> getStepsMap(
      ProgramDefinition programDefinition, Locale preferredLocale) {
    ImmutableMap.Builder<String, String> applicationStepsBuilder = ImmutableMap.builder();
    programDefinition
        .applicationSteps()
        .forEach(
            (step) -> {
              applicationStepsBuilder.put(
                  step.getTitle().getOrDefault(preferredLocale),
                  TextFormatter.formatTextToSanitizedHTML(
                      step.getDescription().getOrDefault(preferredLocale),
                      /* preserveEmptyLines= */ true,
                      /* addRequiredIndicator= */ false));
            });
    ImmutableMap<String, String> applicationStepsMap = applicationStepsBuilder.build();
    return applicationStepsMap;
  }
}
