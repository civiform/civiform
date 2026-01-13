package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.inject.Inject;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.BundledAssetsFinder;
import services.DeploymentType;
import services.LocalizedStrings;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService.ApplicantProgramData;
import services.settings.SettingsManifest;
import views.applicant.ApplicantBaseView;
import views.applicant.ProgramCardsSectionParamsFactory;
import views.applicant.ProgramCardsSectionParamsFactory.ProgramCardParams;

public class ProgramCardPreview extends ApplicantBaseView {
  ProgramCardsSectionParamsFactory programCardsSectionParamsFactory;

  @Inject
  ProgramCardPreview(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      BundledAssetsFinder bundledAssetsFinder,
      ApplicantRoutes applicantRoutes,
      SettingsManifest settingsManifest,
      LanguageUtils languageUtils,
      DeploymentType deploymentType,
      ProgramCardsSectionParamsFactory programCardsSectionParamsFactory) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        bundledAssetsFinder,
        applicantRoutes,
        settingsManifest,
        languageUtils,
        deploymentType);
    this.programCardsSectionParamsFactory = checkNotNull(programCardsSectionParamsFactory);
  }

  public String render(Params params) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(
            params.request(),
            params.applicantId(),
            Optional.of(params.profile()),
            params.applicantPersonalInfo(),
            params.messages());

    ProgramCardParams programCardParams =
        programCardsSectionParamsFactory.getCard(
            params.request(),
            params.messages(),
            MessageKey.BUTTON_VIEW_AND_APPLY,
            params.applicantProgramData(),
            LocalizedStrings.DEFAULT_LOCALE, // Admin console is not localized
            Optional.of(params.profile()),
            params.applicantId(),
            params.applicantPersonalInfo());
    context.setVariable("card", programCardParams);

    return templateEngine.process("admin/programs/ProgramCardPreviewFragment.html", context);
  }

  @AutoValue
  public abstract static class Params {

    public static Builder builder() {
      return new AutoValue_ProgramCardPreview_Params.Builder();
    }

    abstract Request request();

    abstract Optional<Long> applicantId();

    abstract ApplicantPersonalInfo applicantPersonalInfo();

    abstract CiviFormProfile profile();

    abstract ApplicantProgramData applicantProgramData();

    abstract Messages messages();

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setRequest(Request request);

      public abstract Builder setApplicantId(long applicantId);

      public abstract Builder setApplicantPersonalInfo(ApplicantPersonalInfo personalInfo);

      public abstract Builder setProfile(CiviFormProfile profile);

      public abstract Builder setApplicantProgramData(ApplicantProgramData applicantProgramData);

      public abstract Builder setMessages(Messages messages);

      public abstract Params build();
    }
  }
}
