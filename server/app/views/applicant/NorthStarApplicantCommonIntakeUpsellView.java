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

    String linkHref = settingsManifest.getCommonIntakeMoreResourcesLinkHref(params.request()).get();
    String linkText = settingsManifest.getCommonIntakeMoreResourcesLinkText(params.request()).get();
    String linkHtml =
        "<a href=\""
            + linkHref
            + "\" target=\"_blank\" style=\"color: blue; text-decoration: underline;\">"
            + linkText
            + "</a>";
    context.setVariable("moreResourcesLinkHtml", linkHtml);

    ArrayList<DisplayProgram> displayPrograms = new ArrayList<DisplayProgram>();
    Locale userLocale = params.messages().lang().toLocale();
    for (ApplicantProgramData applicantProgramData : params.eligiblePrograms()) {
      String title = applicantProgramData.program().localizedName().getOrDefault(userLocale);
      String description =
          applicantProgramData.program().localizedDescription().getOrDefault(userLocale);
      DisplayProgram bp = new DisplayProgram(title, description);
      displayPrograms.add(bp);
    }
    context.setVariable("eligiblePrograms", displayPrograms);
    context.setVariable("isTrustedIntermediary", params.profile().isTrustedIntermediary());

    return templateEngine.process("applicant/ApplicantCommonIntakeUpsellTemplate", context);
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

  /* Provides syntactic sugar for displaying user-facing program information in HTML. */
  public static class DisplayProgram {
    private final String title;
    private final String description;

    public DisplayProgram(String title, String description) {
      this.title = title;
      this.description = description;
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }
  }
}
