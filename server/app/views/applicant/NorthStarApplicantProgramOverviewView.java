package views.applicant;

import com.google.auto.value.AutoValue;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import java.util.Locale;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.DeploymentType;
import services.applicant.ApplicantPersonalInfo;
import services.program.ProgramDefinition;
import services.settings.SettingsManifest;
import views.NorthStarBaseView;

public class NorthStarApplicantProgramOverviewView extends NorthStarBaseView {

  @Inject
  NorthStarApplicantProgramOverviewView(
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
            Optional.empty(),
            Optional.empty(),
            params.applicantPersonalInfo(),
            params.messages());

    Locale userLocale = params.messages().lang().toLocale();
    ProgramDefinition programDefinition = params.programDefinition();

    String localizedProgramName = programDefinition.localizedName().getOrDefault(userLocale);
    context.setVariable("title", localizedProgramName);

    String description = programDefinition.localizedDescription().getOrDefault(userLocale);
    if (description.isBlank()) {
      description = programDefinition.localizedShortDescription().getOrDefault(userLocale);
    }
    context.setVariable("description", description);
    // TODO: test this with existing programs and add back in filter for blank steps if we need it
    context.setVariable("applicationSteps", programDefinition.applicationSteps());
    context.setVariable("locale", userLocale);

    return templateEngine.process("applicant/ProgramOverviewTemplate", context);
  }

  @AutoValue
  public abstract static class Params {

    public static Builder builder() {
      return new AutoValue_NorthStarApplicantProgramOverviewView_Params.Builder();
    }

    abstract Request request();

    abstract ApplicantPersonalInfo applicantPersonalInfo();

    abstract ProgramDefinition programDefinition();

    abstract Messages messages();

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setRequest(Request request);

      public abstract Builder setApplicantPersonalInfo(ApplicantPersonalInfo applicantPersonalInfo);

      public abstract Builder setProgramDefinition(ProgramDefinition programDefinition);

      public abstract Builder setMessages(Messages messages);

      public abstract Params build();
    }
  }
}
