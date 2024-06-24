package views.applicant;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import java.util.Locale;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.DeploymentType;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applicant.question.ApplicantQuestion;
import services.program.ProgramDefinition;
import services.settings.SettingsManifest;
import views.NorthStarBaseView;

public class NorthStarApplicantIneligibleView extends NorthStarBaseView {

  @Inject
  NorthStarApplicantIneligibleView(
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
    ProgramDefinition program = params.programDefinition();

    Locale userLocale = params.messages().lang().toLocale();
    String localizedProgramName = program.localizedName().getOrDefault(userLocale);
    context.setVariable("programName", localizedProgramName);

    ImmutableList<ApplicantQuestion> ineligibleQuestions =
        params.roApplicantProgramService().getIneligibleQuestions();
    context.setVariable("ineligibleQuestions", ineligibleQuestions);

    // Manually construct a hyperlink with a runtime href and localized string. The hyperlink will
    // be inserted into another localized string in the Thymeleaf template.
    String linkHref =
        program.externalLink().isEmpty()
            ? applicantRoutes.show(params.profile(), params.applicantId(), program.id()).url()
            : program.externalLink();
    String linkText =
        params.messages().at(MessageKey.LINK_PROGRAM_DETAILS.getKeyName()).toLowerCase(Locale.ROOT);
    String linkHtml =
        "<a href=\""
            + linkHref
            + "\" target=\"_blank\" style=\"color: blue; text-decoration: underline;\">"
            + linkText
            + "</a>";
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
