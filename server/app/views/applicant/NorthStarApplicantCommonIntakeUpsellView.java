package views.applicant;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import java.util.ArrayList;
import java.util.Locale;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService.ApplicantProgramData;
import services.settings.SettingsManifest;

public class NorthStarApplicantCommonIntakeUpsellView extends NorthStarApplicantBaseView {

  @Inject
  NorthStarApplicantCommonIntakeUpsellView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes,
      SettingsManifest settingsManifest,
      LanguageUtils languageUtils) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        assetsFinder,
        applicantRoutes,
        settingsManifest,
        languageUtils);
  }

  public String render(Params params) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(
            params.request(),
            params.applicantId(),
            params.profile(),
            params.applicantPersonalInfo(),
            params.messages());

    context.setVariable("applicationId", params.applicationId());
    context.setVariable("messages", params.messages());

    ArrayList<ProgramDetails> programs = new ArrayList<ProgramDetails>();

    System.out.println("=====ssandbekkhaug");
    Locale userLocale = params.messages().lang().toLocale();
    for (ApplicantProgramData apd : params.eligiblePrograms()) {
      String title = apd.program().localizedName().getOrDefault(userLocale);
      String description = apd.program().localizedDescription().getOrDefault(userLocale);
      ProgramDetails program =
          ProgramDetails.builder().setTitle(title).setDescription(description).build();
      programs.add(program);

      System.out.println(program.title());
      System.out.println(program.description());
    }
    context.setVariable("eligiblePrograms", programs);

    return templateEngine.process("applicant/ApplicantCommonIntakeUpsellFragment", context);
  }

  @AutoValue
  public abstract static class Params {

    public static Builder builder() {
      return new AutoValue_NorthStarApplicantCommonIntakeUpsellView_Params.Builder();
    }

    abstract Request request();

    abstract CiviFormProfile profile();

    abstract long applicantId();

    abstract long applicationId();

    abstract ApplicantPersonalInfo applicantPersonalInfo();

    abstract ImmutableList<ApplicantProgramData> eligiblePrograms();

    abstract Messages messages();

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setRequest(Request request);

      public abstract Builder setProfile(CiviFormProfile profile);

      public abstract Builder setApplicantId(long applicantId);

      public abstract Builder setApplicationId(long applicationId);

      public abstract Builder setApplicantPersonalInfo(ApplicantPersonalInfo applicantPersonalInfo);

      public abstract Builder setEligiblePrograms(
          ImmutableList<ApplicantProgramData> eligiblePrograms);

      public abstract Builder setMessages(Messages messages);

      public abstract Params build();
    }
  }

  /** Localized strings */
  @AutoValue
  public abstract static class ProgramDetails {

    public static Builder builder() {
      return new AutoValue_NorthStarApplicantCommonIntakeUpsellView_ProgramDetails.Builder();
    }

    abstract String title();

    abstract String description();

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setTitle(String title);

      public abstract Builder setDescription(String description);

      public abstract ProgramDetails build();
    }
  }
}
