package actions;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import controllers.FlashKey;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.mvc.Action;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.routing.Router;
import repository.ApplicationRepository;
import repository.ProgramRepository;

/**
 * Check if the current application is associated with the latest version of a program. If it is
 * continue on to the original page as normal. If the application is on an older version update the
 * application to the latest version and redirect to the application review page.
 */
public class ProgramFastForwardApplicationAction extends Action.Simple {
  private final ProfileUtils profileUtils;
  private final ProgramRepository programRepository;
  private final ApplicationRepository applicationRepository;

  @Inject
  public ProgramFastForwardApplicationAction(
      ProfileUtils profileUtils,
      ProgramRepository programRepository,
      ApplicationRepository applicationRepository) {
    this.profileUtils = checkNotNull(profileUtils);
    this.programRepository = checkNotNull(programRepository);
    this.applicationRepository = checkNotNull(applicationRepository);
  }

  @Override
  public CompletionStage<Result> call(Request request) {
    String routePattern = request.attrs().get(Router.Attrs.HANDLER_DEF).path();
    RouteExtractor routeExtractor = new RouteExtractor(routePattern, request.path());

    long programId = routeExtractor.getParamLongValue("programId");
    long applicantId = getApplicantId(request, routeExtractor);

    Optional<Long> latestProgramId = programRepository.getMostRecentActiveProgramId(programId);

    if (latestProgramId.isPresent() && latestProgramId.get() > programId) {
      applicationRepository.updateDraftApplicationProgram(applicantId, latestProgramId.get());

      return CompletableFuture.completedFuture(
          redirect(
                  controllers.applicant.routes.ApplicantProgramReviewController.review(
                          latestProgramId.get())
                      .url())
              .flashing(FlashKey.SHOW_FAST_FORWARDED_MESSAGE, "true"));
    }

    return delegate.call(request);
  }

  /** Get the applicantId. Check the user profile first, and fallback to a route parameter */
  private long getApplicantId(Request request, RouteExtractor routeExtractor) {
    Optional<Long> applicantIdOptional = profileUtils.getApplicantId(request);
    return applicantIdOptional.orElseGet(() -> routeExtractor.getParamLongValue("applicantId"));
  }
}
