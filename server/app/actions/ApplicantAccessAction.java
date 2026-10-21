package actions;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.mvc.Action;
import play.mvc.Http.Request;
import play.mvc.Result;

/**
 * Action that returns a 401 unless the signed in user may act on the applicant in the route's
 * {@code applicantId} parameter (the applicant themselves, or a trusted intermediary who manages
 * them).
 *
 * <p>See {@link FileUploadImprovementsEnabledAction} for why this runs as an action.
 */
public class ApplicantAccessAction extends Action.Simple {
  private final ProfileUtils profileUtils;

  @Inject
  public ApplicantAccessAction(ProfileUtils profileUtils) {
    this.profileUtils = checkNotNull(profileUtils);
  }

  @Override
  public CompletionStage<Result> call(Request req) {
    long applicantId = new RouteExtractor(req).getParamLongValue("applicantId");

    return profileUtils
        .currentUserProfile(req)
        .checkAuthorization(applicantId)
        .handle((_, throwable) -> throwable)
        .thenCompose(
            throwable -> {
              if (throwable == null) {
                return delegate.call(req);
              }
              Throwable cause =
                  throwable instanceof CompletionException && throwable.getCause() != null
                      ? throwable.getCause()
                      : throwable;
              if (cause instanceof SecurityException) {
                return CompletableFuture.completedFuture(unauthorized());
              }
              return CompletableFuture.failedFuture(throwable);
            });
  }
}
