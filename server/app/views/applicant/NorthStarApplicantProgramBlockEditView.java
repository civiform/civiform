package views.applicant;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRequestedAction;
import controllers.applicant.ApplicantRoutes;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import models.ApplicantModel.Suffix;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http.Request;
import services.DeploymentType;
import services.applicant.question.AddressQuestion;
import services.cloud.ApplicantFileNameFormatter;
import services.cloud.StorageUploadRequest;
import services.settings.SettingsManifest;
import views.ApplicationBaseViewParams;
import views.NorthStarBaseView;
import views.fileupload.FileUploadViewStrategy;
import views.html.helper.CSRF;
import views.questiontypes.ApplicantQuestionRendererParams;

/** Renders a page for answering questions in a program screen (block). */
public final class NorthStarApplicantProgramBlockEditView extends NorthStarBaseView {
  private final FileUploadViewStrategy fileUploadViewStrategy;

  @Inject
  NorthStarApplicantProgramBlockEditView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes,
      FileUploadViewStrategy fileUploadViewStrategy,
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
    this.fileUploadViewStrategy = fileUploadViewStrategy;
  }

  public String render(Request request, ApplicationBaseViewParams applicationParams) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(
            request,
            applicationParams.applicantId(),
            applicationParams.profile(),
            applicationParams.applicantPersonalInfo(),
            applicationParams.messages());
    context.setVariable("csrfToken", CSRF.getToken(request.asScala()).value());
    context.setVariable("applicationParams", applicationParams);

    context.setVariable(
        "questionRendererParams", getApplicantQuestionRendererParams(applicationParams));
    context.setVariable(
        "submitFormAction", getFormAction(applicationParams, ApplicantRequestedAction.NEXT_BLOCK));
    // Include file upload specific parameters.
    if (applicationParams.block().isFileUpload()) {
      this.addFileUploadParameters(applicationParams, context);

      return templateEngine.process(
          "applicant/ApplicantProgramFileUploadBlockEditTemplate", context);
    } else {
      context.setVariable(
          "previousFormAction",
          getFormAction(applicationParams, ApplicantRequestedAction.PREVIOUS_BLOCK));
      context.setVariable(
          "reviewFormAction",
          getFormAction(applicationParams, ApplicantRequestedAction.REVIEW_PAGE));
      // TODO(#6910): Why am I unable to access static vars directly from Thymeleaf
      context.setVariable("stateAbbreviations", AddressQuestion.STATE_ABBREVIATIONS);
      context.setVariable("nameSuffixOptions", Suffix.values());
      return templateEngine.process("applicant/ApplicantProgramBlockEditTemplate", context);
    }
  }

  private String getFormAction(
      ApplicationBaseViewParams params, ApplicantRequestedAction nextAction) {
    return applicantRoutes
        .updateBlock(
            params.profile(),
            params.applicantId(),
            params.programId(),
            params.block().getId(),
            params.inReview(),
            nextAction)
        .url();
  }

  private String redirectWithFile(
      ApplicationBaseViewParams params, ApplicantRequestedAction nextAction) {
    return params.baseUrl()
        + applicantRoutes
            .updateFile(
                params.profile(),
                params.applicantId(),
                params.programId(),
                params.block().getId(),
                params.inReview(),
                nextAction)
            .url();
  }

  private String getFileUploadSignedRequestKey(ApplicationBaseViewParams params) {
    return ApplicantFileNameFormatter.formatFileUploadQuestionFilename(
        params.applicantId(), params.programId(), params.block().getId());
  }

  // Returns a mapping from Question ID to Renderer params for that question.
  private Map<Long, ApplicantQuestionRendererParams> getApplicantQuestionRendererParams(
      ApplicationBaseViewParams params) {
    AtomicInteger ordinalErrorCount = new AtomicInteger(0);

    return params.block().getQuestions().stream()
        .collect(
            Collectors.toMap(
                question -> question.getQuestionDefinition().getId(),
                question -> {
                  if (question.hasErrors()) {
                    ordinalErrorCount.incrementAndGet();
                  }
                  ApplicantQuestionRendererParams.Builder paramsBuilder =
                      ApplicantQuestionRendererParams.builder()
                          .setMessages(params.messages())
                          .setErrorDisplayMode(params.errorDisplayMode())
                          .setAutofocus(
                              calculateAutoFocusTarget(
                                  params.errorDisplayMode(),
                                  params.block().hasErrors(),
                                  ordinalErrorCount.get()));
                  if (params.block().isFileUpload()) {
                    StorageUploadRequest signedRequest =
                        params
                            .applicantStorageClient()
                            .getSignedUploadRequest(
                                getFileUploadSignedRequestKey(params),
                                redirectWithFile(params, ApplicantRequestedAction.NEXT_BLOCK));
                    paramsBuilder.setSignedFileUploadRequest(signedRequest);
                  }
                  return paramsBuilder.build();
                }));
  }

  // One field at most should be autofocused on the page. If there are errors, it should be the
  // first field with an error of the first question with errors. Prior to the North Star work, if
  // there were no errors, we would focus on the first field of the question selected in the review
  // page. However, the North Star review page has the user choose a block to answer instead of an
  // individual question, so we leave no focus target to avoid skipping content.
  @VisibleForTesting
  static ApplicantQuestionRendererParams.AutoFocusTarget calculateAutoFocusTarget(
      ApplicantQuestionRendererParams.ErrorDisplayMode errorDisplayMode,
      boolean formHasErrors,
      int ordinalErrorCount) {
    if (formHasErrors
        && ApplicantQuestionRendererParams.ErrorDisplayMode.shouldShowErrors(errorDisplayMode)
        && ordinalErrorCount == 1) {
      return ApplicantQuestionRendererParams.AutoFocusTarget.FIRST_ERROR;
    }
    return ApplicantQuestionRendererParams.AutoFocusTarget.NONE;
  }

  private void addFileUploadParameters(
      ApplicationBaseViewParams params, ThymeleafModule.PlayThymeleafContext context) {
    context.setVariable("fileUploadViewStrategy", fileUploadViewStrategy);
    context.setVariable("maxFileSizeMb", params.applicantStorageClient().getFileLimitMb());
    context.setVariable(
        "nextBlockWithFile", redirectWithFile(params, ApplicantRequestedAction.NEXT_BLOCK));
    context.setVariable(
        "previousBlockWithFile", redirectWithFile(params, ApplicantRequestedAction.PREVIOUS_BLOCK));
    context.setVariable(
        "reviewPageWithFile", redirectWithFile(params, ApplicantRequestedAction.REVIEW_PAGE));
    context.setVariable(
        "previousBlockWithoutFile",
        params.baseUrl()
            + applicantRoutes
                .blockPreviousOrReview(
                    params.profile(),
                    params.applicantId(),
                    params.programId(),
                    params.blockIndex(),
                    params.inReview())
                .url());
    context.setVariable(
        "reviewPageWithoutFile",
        params.baseUrl()
            + applicantRoutes
                .review(params.profile(), params.applicantId(), params.programId())
                .url());
  }
}
