package views.admin.questions;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.DeploymentType;
import services.applicant.ApplicantData;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.question.AddressQuestion;
import services.applicant.question.ApplicantQuestion;
import services.program.ProgramQuestionDefinition;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.settings.SettingsManifest;
import views.applicant.NorthStarApplicantBaseView;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode;

public class NorthStarQuestionPreview extends NorthStarApplicantBaseView {

  @Inject
  NorthStarQuestionPreview(
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
    QuestionDefinition questionDefinition;
    try {
      questionDefinition = ApplicantQuestionRendererFactory.questionDefinitionSample(params.type());
    } catch (UnsupportedQuestionTypeException e) {
      System.out.println("Building question failed: " + e.getLocalizedMessage());
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    ProgramQuestionDefinition pqd =
        ProgramQuestionDefinition.create(questionDefinition, Optional.empty());
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(pqd, new ApplicantData(), Optional.empty());
    context.setVariable("question", applicantQuestion);

    ApplicantQuestionRendererParams rendererParams = rendererParams(params);
    context.setVariable("questionRendererParams", rendererParams);
    context.setVariable("stateAbbreviations", AddressQuestion.STATE_ABBREVIATIONS);

    return templateEngine.process("admin/questions/QuestionPreviewFragment", context);
  }

  private ApplicantQuestionRendererParams rendererParams(Params params) {
    ApplicantQuestionRendererParams.Builder paramsBuilder =
        ApplicantQuestionRendererParams.builder()
            .setMessages(params.messages())
            .setErrorDisplayMode(ErrorDisplayMode.HIDE_ERRORS)
            .setAutofocus(ApplicantQuestionRendererParams.AutoFocusTarget.NONE);
    return paramsBuilder.build();
  }

  @AutoValue
  public abstract static class Params {

    public static Builder builder() {
      return new AutoValue_NorthStarQuestionPreview_Params.Builder();
    }

    abstract Request request();

    abstract long applicantId();

    abstract ApplicantPersonalInfo applicantPersonalInfo();

    abstract CiviFormProfile profile();

    abstract QuestionType type();

    abstract Messages messages();

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setRequest(Request request);

      public abstract Builder setApplicantId(long applicantId);

      public abstract Builder setApplicantPersonalInfo(ApplicantPersonalInfo personalInfo);

      public abstract Builder setProfile(CiviFormProfile profile);

      public abstract Builder setType(QuestionType type);

      public abstract Builder setMessages(Messages messages);

      public abstract Params build();
    }
  }
}
