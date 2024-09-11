package actions;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.mvc.Action;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.routing.Router;
import repository.VersionRepository;

/**
 * Check if the profile is authorized to view requested applicant and program data. If no, redirect
 * home. If yes, continue on.
 */
public class ApplicantAuthorizationAction extends Action.Simple {
  private final ProfileUtils profileUtils;
  private final VersionRepository versionRepository;

  @Inject
  public ApplicantAuthorizationAction(
      ProfileUtils profileUtils, VersionRepository versionRepository) {
    this.profileUtils = checkNotNull(profileUtils);
    this.versionRepository = checkNotNull(versionRepository);
  }

  @Override
  public CompletionStage<Result> call(Request request) {
    System.out.println("IN AUTHORIZATION ACTION");
    String routePattern = request.attrs().get(Router.Attrs.HANDLER_DEF).path();
    RouteExtractor routeExtractor = new RouteExtractor(routePattern, request.path());

    long programId = routeExtractor.getParamLongValue("programId");
    long applicantId = getApplicantId(request, routeExtractor);

    return checkApplicantAuthorization(request, applicantId)
        .handle(
            (applicantAuthResult, applicantAuthThrowable) -> {
              if (applicantAuthThrowable != null) {
                return handleException(applicantAuthThrowable);
              }

              return checkProgramAuthorization(request, programId)
                  .handle(
                      (programAuthResult, programAuthThrowable) ->
                          programAuthThrowable != null
                              ? handleException(programAuthThrowable)
                              : delegate.call(request))
                  .thenCompose(resultStage -> resultStage);
            })
        .thenCompose(resultStage -> resultStage);
  }

  private static CompletableFuture<Result> handleException(Throwable throwable) {
    if (throwable instanceof CompletionException) {
      Throwable cause = throwable.getCause();
      if (cause instanceof SecurityException) {
        System.out.println("REDIRECTING TO HOMECONTROLLER BECAUSE OF SECURITY EXCEPTION");
        return CompletableFuture.completedFuture(
            redirect(controllers.routes.HomeController.index().url()));
      }
      throw new RuntimeException(cause);
    }
    throw new RuntimeException(throwable);
  }

  /** Get the applicantId. Check the user profile first, and fallback to a route parameter */
  private long getApplicantId(Request request, RouteExtractor routeExtractor) {
    Optional<Long> applicantIdOptional = profileUtils.getApplicantId(request);
    return applicantIdOptional.orElseGet(() -> routeExtractor.getParamLongValue("applicantId"));
  }

  /** Check if the profile is authorized to access the applicant's data. */
  private CompletionStage<Void> checkApplicantAuthorization(Request request, long applicantId) {
    return profileUtils.currentUserProfile(request).checkAuthorization(applicantId);
  }

  /** Checks that the profile is authorized to access the specified program. */
  private CompletionStage<Void> checkProgramAuthorization(Request request, Long programId) {
    return versionRepository
        .isDraftProgramAsync(programId)
        .thenAccept(
            (isDraftProgram) -> {
              if (isDraftProgram && !profileUtils.currentUserProfile(request).isCiviFormAdmin()) {
                throw new SecurityException();
              }
            });
  }
}
