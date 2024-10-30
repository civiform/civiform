package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import controllers.applicant.routes;
import java.util.Locale;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.AlertSettings;
import services.AlertType;
import services.DeploymentType;
import services.MessageKey;
import services.settings.SettingsManifest;
import views.NorthStarBaseView;
import views.applicant.ProgramCardsSectionParamsFactory.ProgramSectionParams;

public class NorthStarApplicantUpsellView extends NorthStarBaseView {
  private final ProgramCardsSectionParamsFactory programCardsSectionParamsFactory;

  @Inject
  NorthStarApplicantUpsellView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes,
      SettingsManifest settingsManifest,
      LanguageUtils languageUtils,
      DeploymentType deploymentType,
      ProgramCardsSectionParamsFactory programCardsSectionParamsFactory) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        assetsFinder,
        applicantRoutes,
        settingsManifest,
        languageUtils,
        deploymentType);
    this.programCardsSectionParamsFactory = checkNotNull(programCardsSectionParamsFactory);
  }

  public String render(UpsellParams params) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(
            params.request(),
            Optional.of(params.applicantId()),
            Optional.of(params.profile()),
            params.applicantPersonalInfo(),
            params.messages());

    context.setVariable(
        "pageTitle", params.messages().at(MessageKey.TITLE_APPLICATION_CONFIRMATION.getKeyName()));

    context.setVariable("programTitle", params.programTitle().orElse(""));
    context.setVariable("programDescription", params.programDescription().orElse(""));
    context.setVariable("applicationId", params.applicationId());
    context.setVariable("bannerMessage", params.bannerMessage());

    String alertTitle =
        params
            .messages()
            .at(MessageKey.ALERT_SUBMITTED.getKeyName(), params.programTitle().orElse(""));
    AlertSettings successAlertSettings =
        new AlertSettings(
            /* show= */ true, Optional.of(alertTitle), "", AlertType.SUCCESS, ImmutableList.of());
    context.setVariable("successAlertSettings", successAlertSettings);

    String applicantName =
        params.profile().getApplicant().join().getAccount().getApplicantDisplayName();
    context.setVariable("applicantName", applicantName);

    context.setVariable("dateSubmitted", params.dateSubmitted());

    Locale locale = params.messages().lang().toLocale();
    String customConfirmationMessage = params.customConfirmationMessage().getOrDefault(locale);
    context.setVariable("customConfirmationMessage", customConfirmationMessage);

    // Info for login modal
    String applyToProgramsUrl = applicantRoutes.index(params.profile(), params.applicantId()).url();
    context.setVariable("upsellBypassUrl", applyToProgramsUrl);
    context.setVariable(
        "upsellLoginUrl",
        controllers.routes.LoginController.applicantLogin(Optional.of(applyToProgramsUrl)).url());

    String downloadHref =
        routes.UpsellController.download(params.applicationId(), params.applicantId()).url();
    context.setVariable("downloadHref", downloadHref);

    // Create account or login alert
    context.setVariable("createAccountLink", controllers.routes.LoginController.register().url());

    ProgramSectionParams cardsSection =
        programCardsSectionParamsFactory.getSection(
            params.request(),
            params.messages(),
            Optional.empty(),
            MessageKey.BUTTON_APPLY,
            params.eligiblePrograms().get(),
            /* preferredLocale= */ params.messages().lang().toLocale(),
            Optional.of(params.profile()),
            Optional.of(params.applicantId()),
            params.applicantPersonalInfo(),
            ProgramCardsSectionParamsFactory.SectionType.STANDARD);
    context.setVariable("cardsSection", cardsSection);

    return templateEngine.process("applicant/ApplicantUpsellTemplate", context);
  }
}
