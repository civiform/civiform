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
import views.ApplicationBaseViewParams;
import views.html.helper.CSRF;
import views.questiontypes.ApplicantQuestionRendererParams;

/** Renders a page for answering questions in a program screen (block). */
public final class NorthStarApplicantProgramBlockEditView extends NorthStarApplicantBaseView {

  @Inject
  NorthStarApplicantProgramBlockEditView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes) {
    super(templateEngine, playThymeleafContextFactory, assetsFinder, applicantRoutes);
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
    context.setVariable("questionRendererParams", getApplicantQuestionRendererParams(applicationParams));
    return templateEngine.process("applicant/ApplicantProgramBlockEditTemplate", context);
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

  // Returns a mapping from Question ID to Renderer params for that question.
  private Map<Long, ApplicantQuestionRendererParams> getApplicantQuestionRendererParams(
      ApplicationBaseView.Params params) {
    AtomicInteger ordinalErrorCount = new AtomicInteger(0);

    return params.block().getQuestions().stream()
        .collect(
            Collectors.toMap(
                question -> question.getQuestionDefinition().getId(),
                question -> {
                  if (question.hasErrors()) {
                    ordinalErrorCount.incrementAndGet();
                  }
                  return ApplicantQuestionRendererParams.builder()
                      .setMessages(params.messages())
                      .setErrorDisplayMode(params.errorDisplayMode())
                      .setAutofocus(
                          calculateAutoFocusTarget(
                              params.errorDisplayMode(),
                              params.block().hasErrors(),
                              ordinalErrorCount.get()))
                      .build();
                }));
  }

  // One field at most should be autofocused on the page. If there are errors,
  // it should be the first field with an error of the first question with
  // errors.
  @VisibleForTesting
  ApplicantQuestionRendererParams.AutoFocusTarget calculateAutoFocusTarget(
      ApplicantQuestionRendererParams.ErrorDisplayMode errorDisplayMode,
      boolean formHasErrors,
      int ordinalErrorCount) {
    // TODO: If there are no errors, should we focus on the first question?
    if (formHasErrors
        && ApplicantQuestionRendererParams.ErrorDisplayMode.shouldShowErrors(errorDisplayMode)
        && ordinalErrorCount == 1) {
      return ApplicantQuestionRendererParams.AutoFocusTarget.FIRST_ERROR;
    }
    return ApplicantQuestionRendererParams.AutoFocusTarget.NONE;
  }
}
