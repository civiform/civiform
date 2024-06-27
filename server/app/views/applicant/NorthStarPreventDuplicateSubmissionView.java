package views.applicant;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.DeploymentType;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ReadOnlyApplicantProgramService;
import services.settings.SettingsManifest;
import views.NorthStarBaseView;

public class NorthStarPreventDuplicateSubmissionView extends NorthStarBaseView {

  @Inject
  NorthStarPreventDuplicateSubmissionView(
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

  public String render(Params params) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(
            params.request(),
            params.applicantId(),
            params.profile(),
            params.applicantPersonalInfo(),
            params.messages());
    context.setVariable("programName", params.roApplicantProgramService().getProgramTitle());

    String continueEditingHref =
        applicantRoutes
            .review(
                params.profile(),
                params.applicantId(),
                params.roApplicantProgramService().getProgramId())
            .url();
    context.setVariable("continueEditingHref", continueEditingHref);

    String exitHref = applicantRoutes.index(params.profile(), params.applicantId()).url();
    context.setVariable("exitHref", exitHref);

    return templateEngine.process("applicant/PreventDuplicateSubmissionTemplate", context);
  }

  @AutoValue
  public abstract static class Params {

    public static Builder builder() {
      return new AutoValue_NorthStarPreventDuplicateSubmissionView_Params.Builder();
    }

    abstract Request request();

    abstract CiviFormProfile profile();

    abstract long applicantId();

    abstract ApplicantPersonalInfo applicantPersonalInfo();

    abstract ReadOnlyApplicantProgramService roApplicantProgramService();

    abstract Messages messages();

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setRequest(Request request);

      public abstract Builder setProfile(CiviFormProfile profile);

      public abstract Builder setApplicantId(long applicantId);

      public abstract Builder setApplicantPersonalInfo(ApplicantPersonalInfo applicantPersonalInfo);

      public abstract Builder setRoApplicantProgramService(
          ReadOnlyApplicantProgramService rOnlyApplicantProgramService);

      public abstract Builder setMessages(Messages messages);

      public abstract Params build();
    }
  }
}
