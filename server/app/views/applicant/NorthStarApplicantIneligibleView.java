package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import controllers.applicant.EligibilityAlertSettingsCalculator;
import java.util.Locale;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.AlertSettings;
import services.DeploymentType;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ReadOnlyApplicantProgramService;
import services.program.ProgramDefinition;
import services.settings.SettingsManifest;
import views.NorthStarBaseView;

public class NorthStarApplicantIneligibleView extends NorthStarBaseView {
  private final EligibilityAlertSettingsCalculator eligibilityAlertSettingsCalculator;

  @Inject
  NorthStarApplicantIneligibleView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes,
      SettingsManifest settingsManifest,
      LanguageUtils languageUtils,
      DeploymentType deploymentType,
      EligibilityAlertSettingsCalculator eligibilityAlertSettingsCalculator) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        assetsFinder,
        applicantRoutes,
        settingsManifest,
        languageUtils,
        deploymentType);
    this.eligibilityAlertSettingsCalculator = checkNotNull(eligibilityAlertSettingsCalculator);
  }

  public String render(Params params) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(
            params.request(),
            Optional.of(params.applicantId()),
            Optional.of(params.profile()),
            params.applicantPersonalInfo(),
            params.messages());
    ProgramDefinition program = params.programDefinition();

    Locale userLocale = params.messages().lang().toLocale();
    String localizedProgramName = program.localizedName().getOrDefault(userLocale);
    context.setVariable("programName", localizedProgramName);

    String localizedProgramDescription = program.localizedDescription().getOrDefault(userLocale);
    context.setVariable("programDescription", localizedProgramDescription);

    AlertSettings eligibilityAlertSettings =
        eligibilityAlertSettingsCalculator.calculate(
            params.request(),
            params.profile().isTrustedIntermediary(),
            !params.roApplicantProgramService().isApplicationNotEligible(),
            true,
            true,
            program.id(),
            params.roApplicantProgramService().getIneligibleQuestions());
    context.setVariable("eligibilityAlertSettings", eligibilityAlertSettings);

    // Manually construct a hyperlink with a runtime href and localized string. The hyperlink will
    // be inserted into another localized string in the Thymeleaf template.
    // TODO: Update this to point to the new northstar details page.
    String linkHref =
        program.externalLink().isEmpty()
            ? applicantRoutes.review(params.profile(), params.applicantId(), program.id()).url()
            : program.externalLink();
    String linkText =
        params.messages().at(MessageKey.LINK_PROGRAM_DETAILS.getKeyName()).toLowerCase(Locale.ROOT);
    String linkHtml =
        "<a href=\"" + linkHref + "\" target=\"_blank\" class=\"usa-link\">" + linkText + "</a>";
    context.setVariable("programLinkHtml", linkHtml);

    String applyHref = applicantRoutes.index(params.profile(), params.applicantId()).url();
    context.setVariable("applyHref", applyHref);

    String goBackHref =
        applicantRoutes.review(params.profile(), params.applicantId(), program.id()).url();
    context.setVariable("goBackHref", goBackHref);

    return templateEngine.process("applicant/IneligibleTemplate", context);
  }

  @AutoValue
  public abstract static class Params {

    public static Builder builder() {
      return new AutoValue_NorthStarApplicantIneligibleView_Params.Builder();
    }

    abstract Request request();

    abstract CiviFormProfile profile();

    abstract long applicantId();

    abstract ApplicantPersonalInfo applicantPersonalInfo();

    abstract ProgramDefinition programDefinition();

    abstract ReadOnlyApplicantProgramService roApplicantProgramService();

    abstract Messages messages();

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setRequest(Request request);

      public abstract Builder setProfile(CiviFormProfile profile);

      public abstract Builder setApplicantId(long applicantId);

      public abstract Builder setApplicantPersonalInfo(ApplicantPersonalInfo applicantPersonalInfo);

      public abstract Builder setProgramDefinition(ProgramDefinition programDefinition);

      public abstract Builder setRoApplicantProgramService(
          ReadOnlyApplicantProgramService rOnlyApplicantProgramService);

      public abstract Builder setMessages(Messages messages);

      public abstract Params build();
    }
  }
}
