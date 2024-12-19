package views.applicant;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRequestedAction;
import controllers.applicant.ApplicantRoutes;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import models.ApplicantModel.Suffix;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http.Request;
import services.DeploymentType;
import services.MessageKey;
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
  /**
   * This fallback should not ever be reached, but it is here in the event that the {@link
   * SettingsManifest} can't find it in the config to allow for basic functionality to continue.
   * This should be kept in sync with the config value `file_upload_allowed_file_type_specifiers` in
   * the application.conf file.
   */
  private static final String ALLOWED_FILE_TYPE_SPECIFIERS_FALLBACK = "image/*,.pdf";

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
            Optional.of(applicationParams.applicantId()),
            Optional.of(applicationParams.profile()),
            applicationParams.applicantPersonalInfo(),
            applicationParams.messages());
    context.setVariable("csrfToken", CSRF.getToken(request.asScala()).value());
    context.setVariable("applicationParams", applicationParams);

    String pageTitle =
        pageTitleWithBlockProgress(
            applicationParams.programTitle(),
            applicationParams.blockIndex(),
            applicationParams.blockList().size(),
            applicationParams.messages());
    context.setVariable("pageTitle", pageTitle);

    // Progress bar
    ProgressBar progressBar =
        new ProgressBar(
            applicationParams.blockList(),
            applicationParams.blockIndex(),
            applicationParams.messages());
    context.setVariable("progressBar", progressBar);

    Map<Long, ApplicantQuestionRendererParams> questionParams =
        getApplicantQuestionRendererParams(applicationParams);
    context.setVariable("questionRendererParams", questionParams);
    context.setVariable(
        "submitFormAction", getFormAction(applicationParams, ApplicantRequestedAction.NEXT_BLOCK));

    /*
     * Expected flow:
     * 1. On block edit page, user has invalid form
     * 2. User clicks "Back"
     * 3. Block edit page reloads via routes (see getFormAction(...))
     * 4. Block edit page needs to show a modal
     */
    boolean showErrorModal =
        questionParams.values().stream().anyMatch(param -> param.shouldShowErrorsWithModal());
    context.setVariable("showErrorModal", showErrorModal);

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
      context.setVariable(
          "errorModalFormAction",
          getFormAction(applicationParams, ApplicantRequestedAction.PREVIOUS_BLOCK));
      context.setVariable(
          "errorModalTitle", MessageKey.MODAL_ERROR_SAVING_PREVIOUS_TITLE.getKeyName());
      context.setVariable(
          "errorModalContent", MessageKey.MODAL_ERROR_SAVING_PREVIOUS_CONTENT.getKeyName());
      context.setVariable(
          "errorModalButtonText",
          MessageKey.MODAL_ERROR_SAVING_PREVIOUS_NO_SAVE_BUTTON.getKeyName());
      context.setVariable("errorModalDataRedirectTo", previousWithoutSaving(applicationParams));

      if (applicationParams.errorDisplayMode()
          == ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS_WITH_MODAL_REVIEW) {
        context.setVariable(
            "errorModalTitle", MessageKey.MODAL_ERROR_SAVING_REVIEW_TITLE.getKeyName());
        context.setVariable(
            "errorModalContent", MessageKey.MODAL_ERROR_SAVING_REVIEW_CONTENT.getKeyName());
        context.setVariable(
            "errorModalButtonText",
            MessageKey.MODAL_ERROR_SAVING_REVIEW_NO_SAVE_BUTTON.getKeyName());
        context.setVariable(
            "errorModalFormAction",
            getFormAction(applicationParams, ApplicantRequestedAction.REVIEW_PAGE));
        context.setVariable("errorModalDataRedirectTo", reviewWithoutSaving(applicationParams));
      }

      // TODO(#6910): Why am I unable to access static vars directly from Thymeleaf
      context.setVariable("stateAbbreviations", AddressQuestion.STATE_ABBREVIATIONS);
      context.setVariable("nameSuffixOptions", Suffix.values());
      context.setVariable(
          "isNameSuffixEnabled", settingsManifest.getNameSuffixDropdownEnabled(request));
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

  private String redirectWithFile(ApplicationBaseViewParams params) {
    return params.baseUrl()
        + applicantRoutes
            .addFile(
                params.profile(),
                params.applicantId(),
                params.programId(),
                params.block().getId(),
                params.inReview())
            .url();
  }

  private String previousWithoutSaving(ApplicationBaseViewParams params) {
    return params
        .applicantRoutes()
        .blockPreviousOrReview(
            params.profile(),
            params.applicantId(),
            params.programId(),
            params.blockIndex(),
            params.inReview())
        .url();
  }

  private String reviewWithoutSaving(ApplicationBaseViewParams params) {
    return params
        .applicantRoutes()
        .review(params.profile(), params.applicantId(), params.programId())
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
                                getFileUploadSignedRequestKey(params), redirectWithFile(params));
                    paramsBuilder.setSignedFileUploadRequest(signedRequest);
                  }
                  return paramsBuilder.build();
                }));
  }

  // One field at most should be autofocused on the page. If there are errors, it
  // should be the first field with an error of the first question with errors.
  // Prior to the North Star work, if there were no errors, we would focus on the
  // first field of the question selected in the review page. However, the North
  // Star review page has the user choose a block to answer instead of an
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
        "fileUploadAllowedFileTypeSpecifiers",
        settingsManifest
            .getFileUploadAllowedFileTypeSpecifiers()
            .orElse(ALLOWED_FILE_TYPE_SPECIFIERS_FALLBACK));
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
