package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import auth.UatProfile;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Application;
import org.pac4j.play.java.Secure;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Call;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.applicant.AnswerData;
import services.applicant.ApplicantService;
import services.applicant.Block;
import services.applicant.exception.ApplicationSubmissionException;
import services.program.ProgramNotFoundException;
import views.applicant.ApplicantProgramConfirmationView;
import views.applicant.ApplicantProgramSummaryView;

/**
 * Controller for reviewing program responses for an applicant.
 *
 * <p>CAUTION: You must explicitly check the current profile so that an unauthorized user cannot
 * access another applicant's data!
 */
public class ApplicantProgramReviewController extends CiviFormController {

  private final ApplicantService applicantService;
  private final HttpExecutionContext httpExecutionContext;
  private final MessagesApi messagesApi;
  private final ApplicantProgramSummaryView summaryView;
  private final ProfileUtils profileUtils;
  private final ApplicantProgramConfirmationView confirmationView;

  @Inject
  public ApplicantProgramReviewController(
      ApplicantService applicantService,
      HttpExecutionContext httpExecutionContext,
      MessagesApi messagesApi,
      ApplicantProgramSummaryView summaryView,
      ApplicantProgramConfirmationView applicantProgramConfirmationView,
      ProfileUtils profileUtils) {
    this.applicantService = checkNotNull(applicantService);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.messagesApi = checkNotNull(messagesApi);
    this.summaryView = checkNotNull(summaryView);
    this.profileUtils = checkNotNull(profileUtils);
    this.confirmationView = checkNotNull(applicantProgramConfirmationView);
  }

  @Secure
  public CompletionStage<Result> review(Request request, long applicantId, long programId) {
    Optional<String> banner = request.flash().get("banner");
    CompletionStage<String> applicantStage = this.applicantService.getName(applicantId);

    return applicantStage
        .thenComposeAsync(v -> checkApplicantAuthorization(profileUtils, request, applicantId))
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            httpExecutionContext.current())
        .thenApplyAsync(
            (roApplicantProgramService) -> {
              ImmutableList<AnswerData> summaryData = roApplicantProgramService.getSummaryData();
              int totalBlockCount = roApplicantProgramService.getAllBlocks().size();
              int completedBlockCount =
                  roApplicantProgramService.getAllBlocks().stream()
                      .filter(Block::isCompleteWithoutErrors)
                      .mapToInt(b -> 1)
                      .sum();
              String programTitle = roApplicantProgramService.getProgramTitle();
              return ok(
                  summaryView.render(
                      request,
                      applicantId,
                      applicantStage.toCompletableFuture().join(),
                      programId,
                      programTitle,
                      summaryData,
                      completedBlockCount,
                      totalBlockCount,
                      messagesApi.preferred(request),
                      banner));
            },
            httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof SecurityException) {
                  return unauthorized();
                }
                if (cause instanceof ProgramNotFoundException) {
                  return notFound(cause.toString());
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }

  @Secure
  public CompletionStage<Result> submit(Request request, long applicantId, long programId) {
    return checkApplicantAuthorization(profileUtils, request, applicantId)
        .thenComposeAsync(
            v -> {
              return submitInternal(request, applicantId, programId);
            },
            httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof SecurityException) {
                  return unauthorized();
                }
                if (cause instanceof ProgramNotFoundException) {
                  return notFound(cause.toString());
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }

  @Secure
  public CompletionStage<Result> confirmation(
      Request request, long applicantId, long programId, long applicationId) {
    CompletionStage<String> applicantStage = this.applicantService.getName(applicantId);

    return applicantStage
        .thenComposeAsync(v -> checkApplicantAuthorization(profileUtils, request, applicantId))
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            httpExecutionContext.current())
        .thenApplyAsync(
            (roApplicantProgramService) -> {
              String programTitle = roApplicantProgramService.getProgramTitle();
              Optional<String> banner = request.flash().get("banner");
              return ok(
                  confirmationView.render(
                      request,
                      applicantId,
                      applicantStage.toCompletableFuture().join(),
                      applicationId,
                      programTitle,
                      messagesApi.preferred(request),
                      banner));
            },
            httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof SecurityException) {
                  return unauthorized();
                }
                if (cause instanceof ProgramNotFoundException) {
                  return notFound(cause.toString());
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }

  private CompletionStage<Result> submitInternal(
      Request request, long applicantId, long programId) {
    UatProfile submittingProfile = profileUtils.currentUserProfile(request).orElseThrow();

    CompletionStage<Application> submitApp =
        applicantService.submitApplication(applicantId, programId, submittingProfile);
    return submitApp
        .thenApplyAsync(
            application -> {
              Long applicationId = application.id;
              Call endOfProgramSubmission =
                  routes.RedirectController.considerRegister(
                      routes.ApplicantProgramReviewController.confirmation(
                              applicantId, programId, applicationId)
                          .url());
              return found(endOfProgramSubmission);
            },
            httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof ApplicationSubmissionException) {
                  Call reviewPage =
                      routes.ApplicantProgramReviewController.review(applicantId, programId);
                  return found(reviewPage).flashing("banner", "Error saving application.");
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }
}
