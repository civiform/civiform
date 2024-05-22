package views.applicant;

import annotations.BindingAnnotations;
import com.google.auto.value.AutoValue;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.applicant.ApplicantRoutes;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http.Request;

public class NorthStarApplicantUpsellView extends NorthStarApplicantBaseView {

  private final String authProviderName;

  @Inject
  NorthStarApplicantUpsellView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes,
      @BindingAnnotations.ApplicantAuthProviderName String authProviderName) {
    super(templateEngine, playThymeleafContextFactory, assetsFinder, applicantRoutes);
    this.authProviderName = authProviderName;
  }

  public String render(Request request, Params applicationParams) {
    ThymeleafModule.PlayThymeleafContext context = createThymeleafContext(request);
    // context.setVariable("formAction", getFormAction(applicationParams));
    // context.setVariable("csrfToken", CSRF.getToken(request.asScala()).value());
    context.setVariable("programName", applicationParams.programTitle());
    context.setVariable("authProviderName", authProviderName);
    return templateEngine.process("applicant/ApplicantUpsellFragment", context);
  }

  @AutoValue
  public abstract static class Params {

    public static Builder builder() {
      return new AutoValue_NorthStarApplicantUpsellView_Params.Builder();
    }

    abstract String programTitle();

    abstract long applicationId();

    // abstract String authProviderName();

    // abstract long applicantId();

    // abstract ImmutableList<Block> blocks();

    // abstract int completedBlockCount();

    // abstract CiviFormProfile profile();

    // abstract long programId();

    // abstract int totalBlockCount();

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setProgramTitle(String programTitle);

      public abstract Builder setApplicationId(long applicationId);

      // public abstract Builder setAuthProviderName(String authProviderName);

      // public abstract Builder setApplicantId(long applicantId);

      // public abstract Builder setBlocks(ImmutableList<Block> blocks);

      // public abstract Builder setCompletedBlockCount(int completedBlockCount);

      // public abstract Builder setProfile(CiviFormProfile profile);

      // public abstract Builder setProgramId(long programId);

      // public abstract Builder setTotalBlockCount(int totalBlockCount);

      public abstract Params build();
    }
  }
}
