package views.applicant;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.applicant.ApplicantRequestedAction;
import controllers.applicant.ApplicantRoutes;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http.Request;
import services.applicant.question.AddressQuestion;
import services.cloud.ApplicantFileNameFormatter;
import services.cloud.StorageUploadRequest;
import views.ApplicationBaseViewParams;
import views.html.helper.CSRF;
import views.questiontypes.ApplicantQuestionRendererParams;
import views.fileupload.FileUploadViewStrategy;

/** Renders a page for answering questions in a program screen (block). */
public final class NorthStarApplicantProgramBlockEditView extends NorthStarApplicantBaseView {
  private final FileUploadViewStrategy fileUploadViewStrategy;

  @Inject
  NorthStarApplicantProgramBlockEditView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes,
      FileUploadViewStrategy fileUploadViewStrategy) {
    super(templateEngine, playThymeleafContextFactory, assetsFinder, applicantRoutes);
    this.fileUploadViewStrategy = fileUploadViewStrategy;
  }

  public String render(Request request, ApplicationBaseViewParams applicationParams) {
    ThymeleafModule.PlayThymeleafContext context = createThymeleafContext(request);
    context.setVariable(
        "submitFormAction", getFormAction(applicationParams, ApplicantRequestedAction.NEXT_BLOCK));
    context.setVariable(
        "previousFormAction",
        getFormAction(applicationParams, ApplicantRequestedAction.PREVIOUS_BLOCK));
    context.setVariable(
        "reviewFormAction", getFormAction(applicationParams, ApplicantRequestedAction.REVIEW_PAGE));
    context.setVariable("csrfToken", CSRF.getToken(request.asScala()).value());
    context.setVariable("applicationParams", applicationParams);
    // TODO(#6910): Why am I unable to access static vars directly from Thymeleaf
    context.setVariable("stateAbbreviations", AddressQuestion.STATE_ABBREVIATIONS);
    context.setVariable(
        "questionRendererParams", getApplicantQuestionRendererParams(applicationParams));
    // TODO include signed request
    // Include file upload specific parameters.
    if (applicationParams.block().isFileUpload()) {
      context.setVariable(
          "fileUploadViewStrategy", fileUploadViewStrategy);
    }
    return templateEngine.process("applicant/ApplicantProgramBlockEditTemplate", context);
  }

  private String getFormAction(
      ApplicationBaseViewParams params, ApplicantRequestedAction nextAction) {
    if (params.block().isFileUpload()) {
      return applicantRoutes
      .updateFile(
          params.profile(),
          params.applicantId(),
          params.programId(),
          params.block().getId(),
          params.inReview(),
          nextAction)
      .url();
    }
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

  private String getFileUploadSuccessUrl(ApplicationBaseViewParams params) {
    return params.baseUrl()
        + applicantRoutes
            .updateFile(
                params.profile(),
                params.applicantId(),
                params.programId(),
                params.block().getId(),
                params.inReview(),
                ApplicantRequestedAction.NEXT_BLOCK)
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
                                getFileUploadSuccessUrl(params));
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
}
