package filters;

import static com.google.common.base.Preconditions.checkNotNull;
import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;
import static play.mvc.Results.redirect;

import akka.stream.Materializer;
import auth.GuestClient;
import auth.ProfileUtils;
import com.google.inject.Inject;
import controllers.routes;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import play.mvc.Filter;
import play.mvc.Http;
import play.mvc.Result;

/**
 * Filter that ensures that sessions for user-facing requests have a CiviFormProfile.
 *
 * <p>The filter will redirect the request to the GuestClient callback under these conditions:
 *
 * <p>
 *
 * <ul>
 *   <li>The request is for a user-facing route
 *   <li>The request uses the `GET` method (since we redirect)
 *   <li>The session associated with the request does not contain a pac4j user profile
 * </ul>
 */
public final class CiviFormProfileFilter extends Filter {
  private final ProfileUtils profileUtils;

  @Inject
  public CiviFormProfileFilter(Materializer mat, ProfileUtils profileUtils) {
    super(mat);
    this.profileUtils = checkNotNull(profileUtils);
  }

  private boolean profileIsMissing(Http.RequestHeader requestHeader) {
    return profileUtils.currentUserProfile(requestHeader).isEmpty();
  }

  private boolean shouldApplyThisFilter(Http.RequestHeader requestHeader) {
    return NonUserRoutePrefixes.noneMatch(requestHeader)
        && !requestHeader.path().startsWith("/callback")
        && profileIsMissing(requestHeader);
  }

  @Override
  public CompletionStage<Result> apply(
      Function<Http.RequestHeader, CompletionStage<Result>> nextFilter,
      Http.RequestHeader requestHeader) {
    if (!shouldApplyThisFilter(requestHeader)) {
      return nextFilter.apply(requestHeader);
    }

    // No profile is present.
    //
    // Directly invoke the callback of the GuestClient, which creates a profile. Then redirect the
    // user to the page they were trying to reach.
    return CompletableFuture.completedFuture(
        redirect(routes.CallbackController.callback(GuestClient.CLIENT_NAME).url())
            .withSession(
                requestHeader.session().adding(REDIRECT_TO_SESSION_KEY, requestHeader.uri())));
  }
}
