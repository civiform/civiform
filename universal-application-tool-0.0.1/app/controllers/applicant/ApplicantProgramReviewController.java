package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Application;
import org.pac4j.play.java.Secure;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Call;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.ApplicationRepository;
import services.applicant.AnswerData;
import services.applicant.ApplicantService;
import services.program.ProgramNotFoundException;
import views.applicant.ApplicantProgramSummaryView;

/**
 * Controller for reviewing program responses for an applicant.
 *
 * <p>CAUTION: You must explicitly check the current profile so that an unauthorized user cannot
 * access another applicant's data!
 */
public class ApplicantProgramReviewController extends CiviFormController {

  private final ApplicantService applicantService;
  private final ApplicationRepository applicationRepository;
  private final HttpExecutionContext httpExecutionContext;
  private final ApplicantProgramSummaryView summaryView;
  private final ProfileUtils profileUtils;

  @Inject
  public ApplicantProgramReviewController(
      ApplicantService applicantService,
      ApplicationRepository applicationRepository,
      HttpExecutionContext httpExecutionContext,
      ApplicantProgramSummaryView summaryView,
      ProfileUtils profileUtils) {
    this.applicantService = checkNotNull(applicantService);
    this.applicationRepository = checkNotNull(applicationRepository);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.summaryView = checkNotNull(summaryView);
    this.profileUtils = checkNotNull(profileUtils);
  }

  @Secure
  public CompletionStage<Result> review(Request request, long applicantId, long programId) {
    Optional<String> banner = request.flash().get("banner");
    return checkApplicantAuthorization(profileUtils, request, applicantId)
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            httpExecutionContext.current())
        .thenApplyAsync(
            (roApplicantProgramService) -> {
              ImmutableList<AnswerData> summaryData = roApplicantProgramService.getSummaryData();
              // TODO: Get program title. (Currently no way to do that from
              // roApplicantProgramService)
              String programTitle = "Program title";
              return ok(
                  summaryView.render(
                      request, applicantId, programId, programTitle, summaryData, banner));
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
              return submit(applicantId, programId);
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

  private CompletionStage<Result> submit(long applicantId, long programId) {

    return applicationRepository
        .submitApplication(applicantId, programId)
        .thenApplyAsync(
            applicationMaybe -> {
              if (applicationMaybe.isEmpty()) {
                Call reviewPage =
                    routes.ApplicantProgramReviewController.review(applicantId, programId);
                return found(reviewPage).flashing("banner", "Error saving application.");
              }
              Call endOfProgramSubmission = routes.ApplicantProgramsController.index(applicantId);
              Application application = applicationMaybe.get();
              // Placeholder application ID display.
              return found(endOfProgramSubmission)
                  .flashing(
                      "banner",
                      String.format(
                          "Successfully saved application: application ID %d", application.id));
            });
  }
}
