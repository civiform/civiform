package views.admin.questions;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import forms.questions.EnumeratorQuestionForm;
import java.time.Clock;
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
import services.applicant.RepeatedEntity;
import services.applicant.question.AddressQuestion;
import services.applicant.question.ApplicantQuestion;
import services.program.ProgramQuestionDefinition;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.settings.SettingsManifest;
import views.applicant.ApplicantBaseView;
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
      DeploymentType deploymentType,
      Clock clock) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        bundledAssetsFinder,
        applicantRoutes,
        settingsManifest,
        languageUtils,
        deploymentType,
        clock);
  }

  public String render(Params params) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(
            params.request(),
            Optional.of(params.applicantId()),
            Optional.of(params.profile()),
            params.applicantPersonalInfo(),
            params.messages());
    QuestionDefinition questionDefinition =
        params
            .questionDefinition()
            .orElseGet(
                () -> {
                  try {
                    return QuestionDefinition.questionDefinitionSample(params.type());
                  } catch (UnsupportedQuestionTypeException e) {
                    throw new RuntimeException(e);
                  }
                });
    ApplicantQuestion applicantQuestion =
        previewQuestion(questionDefinition, Optional.empty(), /* optional= */ true);
    context.setVariable("question", applicantQuestion);

    ApplicantQuestionRendererParams rendererParams = rendererParams(params);
    context.setVariable("questionRendererParams", rendererParams);
    if (questionDefinition instanceof EnumeratorQuestionDefinition enumeratorDefinition
        && params.initialQuestionDefinition().isPresent()
        && settingsManifest.getEnumeratorImprovementsEnabled(params.request())) {
      setInitialQuestionContext(
          context,
          applicantQuestion,
          enumeratorDefinition,
          params.initialQuestionDefinition().get(),
          rendererParams);
    }
    context.setVariable("stateAbbreviations", AddressQuestion.STATE_ABBREVIATIONS);
    context.setVariable("enumMaxEntityCount", EnumeratorQuestionForm.MAX_ENUM_ENTITIES_ALLOWED);

    context.setVariable(
        "isNameSuffixEnabled", settingsManifest.getNameSuffixDropdownEnabled(params.request()));
    context.setVariable("nameSuffixOptions", ApplicantModel.Suffix.values());
    context.setVariable("maxFileSizeMb", 100);
    context.setVariable(
        "fileUploadAllowedFileTypeSpecifiers",
        settingsManifest.getFileUploadAllowedFileTypeSpecifiers().orElse("image/*,.pdf"));
    context.setVariable("isPreview", true);
    context.setVariable("homeUrl", index(params, applicantRoutes));
    context.setVariable(
        "imagesInQuestionFeatureEnabled",
        settingsManifest.getImagesInQuestionFeatureEnabled(params.request()));
    // For static question image preview: set the resolved public URL and alt text so the
    // StaticTextQuestionFragment can render an inline 16:9 image block when one is present.
    context.setVariable("questionImageUrl", params.imageUrl().orElse(""));
    context.setVariable("questionImageAltText", params.imageAltText());
    context.setVariable("previewUrl", params.request().uri());
    return templateEngine.process("admin/questions/QuestionPreviewFragment", context);
  }

  /**
   * Adds the variables the enumerator fragment needs to show the initial question: its type and the
   * URL the Add button posts to for a new row. Like a new application, the preview starts with no
   * rows until Add is clicked.
   */
  private void setInitialQuestionContext(
      ThymeleafModule.PlayThymeleafContext context,
      ApplicantQuestion enumeratorApplicantQuestion,
      EnumeratorQuestionDefinition enumeratorDefinition,
      QuestionDefinition initialQuestionDefinition,
      ApplicantQuestionRendererParams rendererParams) {
    context.setVariable("initialQuestionType", initialQuestionDefinition.getQuestionType());
    context.setVariable("contextualizedInitialQuestions", ImmutableList.of());
    context.setVariable(
        "enumeratorQuestion", enumeratorApplicantQuestion.createEnumeratorQuestion());
    context.setVariable(
        "hxAddEnumeratorEntityUrl",
        controllers.admin.routes.QuestionPreviewController.hxAddEnumeratorEntity(
                enumeratorDefinition.getId())
            .url());
    context.setVariable("initialQuestionRendererParams", rendererParams);
  }

  /**
   * Renders one new row for the initial question, for the Add button in the enumerator preview. The
   * row has no answers, so it never shows errors.
   */
  public String renderNewEnumeratorEntityRow(
      Request request,
      Messages messages,
      EnumeratorQuestionDefinition enumeratorDefinition,
      QuestionDefinition initialQuestionDefinition,
      int entityIndex,
      String enumeratorQuestionId) {
    ThymeleafModule.PlayThymeleafContext context = playThymeleafContextFactory.create(request);
    ApplicantQuestion contextualizedInitialQuestion =
        contextualizedInitialQuestion(enumeratorDefinition, initialQuestionDefinition, entityIndex);
    context.setVariable("contextualizedInitialQuestion", contextualizedInitialQuestion);
    context.setVariable("initialQuestionType", contextualizedInitialQuestion.getType());
    context.setVariable(
        "enumeratorQuestion",
        previewQuestion(enumeratorDefinition, Optional.empty(), /* optional= */ true)
            .createEnumeratorQuestion());
    context.setVariable("entityIndex", entityIndex);
    context.setVariable("questionId", enumeratorQuestionId);
    context.setVariable("stateAbbreviations", AddressQuestion.STATE_ABBREVIATIONS);
    context.setVariable("nameSuffixOptions", ApplicantModel.Suffix.values());
    context.setVariable(
        "isNameSuffixEnabled", settingsManifest.getNameSuffixDropdownEnabled(request));
    context.setVariable(
        "initialQuestionRendererParams",
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setErrorDisplayMode(ErrorDisplayMode.HIDE_ERRORS)
            .setAutofocus(ApplicantQuestionRendererParams.AutoFocusTarget.NONE)
            .build());
    return templateEngine.process("questiontypes/EnumeratorNewEntityRowPartial", context);
  }

  private static ApplicantQuestion contextualizedInitialQuestion(
      EnumeratorQuestionDefinition enumeratorDefinition,
      QuestionDefinition initialQuestionDefinition,
      int entityIndex) {
    // The initial question is required, as it is once it is added to a program block. That also
    // makes the Add button wait for a value in the last row.
    return previewQuestion(
        initialQuestionDefinition,
        Optional.of(RepeatedEntity.createForPreview(enumeratorDefinition, entityIndex)),
        /* optional= */ false);
  }

  /** An unanswered question, as it would look to an applicant, for the preview. */
  private static ApplicantQuestion previewQuestion(
      QuestionDefinition questionDefinition,
      Optional<RepeatedEntity> repeatedEntity,
      boolean optional) {
    ProgramQuestionDefinition pqd =
        ProgramQuestionDefinition.create(
            questionDefinition,
            /* programDefinitionId= */ Optional.empty(),
            optional,
            /* addressCorrectionEnabled= */ false);
    return new ApplicantQuestion(pqd, new ApplicantModel(), new ApplicantData(), repeatedEntity);
  }

  private ApplicantQuestionRendererParams rendererParams(Params params) {
    ApplicantQuestionRendererParams.Builder paramsBuilder =
        ApplicantQuestionRendererParams.builder()
            .setMessages(params.messages())
            .setErrorDisplayMode(ErrorDisplayMode.HIDE_ERRORS)
            .setAutofocus(ApplicantQuestionRendererParams.AutoFocusTarget.NONE);
    params.imageUrl().ifPresent(paramsBuilder::setQuestionImageUrl);
    if (!params.imageAltText().isEmpty()) {
      paramsBuilder.setQuestionImageAltText(params.imageAltText());
    }
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
      return new AutoValue_QuestionPreview_Params.Builder()
          .setInitialQuestionDefinition(Optional.empty());
    }

    abstract Request request();

    abstract long applicantId();

    abstract ApplicantPersonalInfo applicantPersonalInfo();

    abstract CiviFormProfile profile();

    abstract QuestionType type();

    abstract Messages messages();

    /** Resolved public URL for the question image, empty when no image is set. */
    abstract Optional<String> imageUrl();

    /** Alt text for the question image, empty string when no image is set. */
    abstract String imageAltText();

    abstract Optional<QuestionDefinition> questionDefinition();

    /** For an enumerator question, its initial question, when it has one. */
    abstract Optional<QuestionDefinition> initialQuestionDefinition();

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setRequest(Request request);

      public abstract Builder setApplicantId(long applicantId);

      public abstract Builder setApplicantPersonalInfo(ApplicantPersonalInfo personalInfo);

      public abstract Builder setProfile(CiviFormProfile profile);

      public abstract Builder setType(QuestionType type);

      public abstract Builder setMessages(Messages messages);

      public abstract Builder setImageUrl(Optional<String> imageUrl);

      public abstract Builder setImageAltText(String imageAltText);

      public abstract Builder setQuestionDefinition(
          Optional<QuestionDefinition> questionDefinition);

      public abstract Builder setInitialQuestionDefinition(
          Optional<QuestionDefinition> initialQuestionDefinition);

      public abstract Params build();
    }
  }
}
