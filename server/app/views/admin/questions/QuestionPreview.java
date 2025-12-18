package views.admin.questions;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.inject.Inject;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import forms.EnumeratorQuestionForm;
import java.util.Optional;
import models.ApplicantModel;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.BundledAssetsFinder;
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
import views.ApplicantBaseView;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode;

public class QuestionPreview extends ApplicantBaseView {

  @Inject
  QuestionPreview(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      ApplicantRoutes applicantRoutes,
      BundledAssetsFinder bundledAssetsFinder,
      SettingsManifest settingsManifest,
      LanguageUtils languageUtils,
      DeploymentType deploymentType) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        bundledAssetsFinder,
        applicantRoutes,
        settingsManifest,
        languageUtils,
        deploymentType);
  }

  public String render(Params params) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(
            params.request(),
            Optional.of(params.applicantId()),
            Optional.of(params.profile()),
            params.applicantPersonalInfo(),
            params.messages());
    QuestionDefinition questionDefinition;
    try {
      questionDefinition = QuestionDefinition.questionDefinitionSample(params.type());
    } catch (UnsupportedQuestionTypeException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    ProgramQuestionDefinition pqd =
        ProgramQuestionDefinition.create(
            questionDefinition,
            /* programDefinitionId= */ Optional.empty(),
            /* optional= */ true,
            /* addressCorrectionEnabled= */ false);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(pqd, new ApplicantModel(), new ApplicantData(), Optional.empty());
    context.setVariable("question", applicantQuestion);

    ApplicantQuestionRendererParams rendererParams = rendererParams(params);
    context.setVariable("questionRendererParams", rendererParams);
    context.setVariable("stateAbbreviations", AddressQuestion.STATE_ABBREVIATIONS);
    context.setVariable("enumMaxEntityCount", EnumeratorQuestionForm.MAX_ENUM_ENTITIES_ALLOWED);

    context.setVariable(
        "isNameSuffixEnabled", settingsManifest.getNameSuffixDropdownEnabled(params.request()));
    context.setVariable("nameSuffixOptions", ApplicantModel.Suffix.values());
    context.setVariable("isYesNoQuestionEnabled", settingsManifest.getYesNoQuestionEnabled());
    context.setVariable("isPreview", true);
    context.setVariable("homeUrl", index(params, applicantRoutes));
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

  // used to find the homepage for a given user
  private String index(Params params, ApplicantRoutes routes) {
    // index() does the TI evaluation.
    return routes.index(params.profile(), params.applicantId()).url();
  }

  @AutoValue
  public abstract static class Params {

    public static Builder builder() {
      return new AutoValue_QuestionPreview_Params.Builder();
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
