package views.applicant;

import com.google.auto.value.AutoValue;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http.Request;
import services.settings.SettingsManifest;

public class NorthStarApplicantUpsellView extends NorthStarApplicantBaseView {

  @Inject
  NorthStarApplicantUpsellView(
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

  public String render(Request request, Params applicationParams) {
    ThymeleafModule.PlayThymeleafContext context = playThymeleafContextFactory.create(request);
    context.setVariable("programName", applicationParams.programTitle());
    context.setVariable("applicationId", applicationParams.applicationId());
    return templateEngine.process("applicant/ApplicantUpsellFragment", context);
  }

  @AutoValue
  public abstract static class Params {

    public static Builder builder() {
      return new AutoValue_NorthStarApplicantUpsellView_Params.Builder();
    }

    abstract String programTitle();

    abstract long applicationId();

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setProgramTitle(String programTitle);

      public abstract Builder setApplicationId(long applicationId);

      public abstract Params build();
    }
  }
}
