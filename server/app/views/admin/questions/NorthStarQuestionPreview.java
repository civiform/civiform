package views.admin.questions;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
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
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.question.AddressQuestion;
import services.applicant.question.ApplicantQuestion;
import services.program.ProgramQuestionDefinition;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;
import services.settings.SettingsManifest;
import views.applicant.NorthStarApplicantBaseView;
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

    System.out.println("Got type: " + params.type());

    QuestionDefinition questionDefinition = questionDefinitionSample(params.type());
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

  // TODO: merge with original from ApplicantQuestionRendererFactory?
  private static QuestionDefinition questionDefinitionSample(QuestionType questionType) {
    QuestionDefinitionBuilder builder =
        new QuestionDefinitionBuilder()
            .setId(1L)
            .setName("")
            .setDescription("")
            .setQuestionText(LocalizedStrings.of(Locale.US, "Sample question text"))
            .setQuestionType(questionType);
    // TODO ssandbekkhaug QuestionText is not set (it appears as "Sample question text"). How to fix
    // this?

    if (questionType.isMultiOptionType()) {
      builder.setQuestionOptions(
          ImmutableList.of(
              QuestionOption.create(
                  1L,
                  1L,
                  "sample option admin name",
                  LocalizedStrings.of(Locale.US, "Sample question option"))));
    }

    if (questionType.equals(QuestionType.ENUMERATOR)) {
      builder.setEntityType(LocalizedStrings.withDefaultValue("Sample repeated entity type"));
    }

    try {
      return builder.build();
    } catch (UnsupportedQuestionTypeException e) {
      System.out.println("Building question failed: " + e.getLocalizedMessage());
      // TODO: better exception handling (copying QuestionPreview.java)
      throw new RuntimeException(e);
    }
  }

  private ApplicantQuestionRendererParams rendererParams(Params params) {
    ApplicantQuestionRendererParams.Builder paramsBuilder =
        ApplicantQuestionRendererParams.builder()
            .setMessages(params.messages())
            .setErrorDisplayMode(ErrorDisplayMode.HIDE_ERRORS)
            .setAutofocus(ApplicantQuestionRendererParams.AutoFocusTarget.NONE);
    // if (params.block().isFileUpload()) {
    //   StorageUploadRequest signedRequest =
    //       params
    //           .applicantStorageClient()
    //           .getSignedUploadRequest(
    //               getFileUploadSignedRequestKey(params),
    //               redirectWithFile(params, ApplicantRequestedAction.NEXT_BLOCK));
    //   paramsBuilder.setSignedFileUploadRequest(signedRequest);
    // }
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
