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
 * Ensures that user-facing requests have a CiviFormProfile by redirecting to create a guest session
 * then redirecting back to the original request.
 */
public final class CiviFormProfileFilter extends Filter {
  private final ProfileUtils profileUtils;

  @Inject
  public CiviFormProfileFilter(Materializer mat, ProfileUtils profileUtils) {
    super(mat);
    this.profileUtils = checkNotNull(profileUtils);
  }

  /**
   * Determines whether to redirect this request to create a guest profile. Returns true if:
   *
   * <ul>
   *   <li>The request is for a user-facing route
   *   <li>The request is not for the homepage (/ or /programs)
   *   <li>The request uses the `GET` or `HEAD` method (POST cannot be redirected back to the
   *       original URI)
   *   <li>The session associated with the request does not contain a pac4j user profile
   * </ul>
   */
  private boolean shouldRedirect(Http.RequestHeader requestHeader) {
    return NonUserRoutes.noneMatch(requestHeader)
        && OptionalProfileRoutes.noneMatch(requestHeader)
        && !requestHeader.path().startsWith("/callback")
        // TODO(#8504) extend to all HTTP methods
        && (requestHeader.method().equals("GET") || requestHeader.method().equals("HEAD"))
        && profileUtils.optionalCurrentUserProfile(requestHeader).isEmpty();
  }

  @Override
  public CompletionStage<Result> apply(
      Function<Http.RequestHeader, CompletionStage<Result>> nextFilter,
      Http.RequestHeader requestHeader) {
    if (shouldRedirect(requestHeader)) {
      // Directly invoke the callback of the GuestClient, which creates a profile. Then redirect the
      // user to the page they were trying to reach.
      return CompletableFuture.completedFuture(
          redirect(routes.CallbackController.callback(GuestClient.CLIENT_NAME).url())
              .withSession(
                  requestHeader.session().adding(REDIRECT_TO_SESSION_KEY, requestHeader.uri())));
    }

    // Do nothing
    return nextFilter.apply(requestHeader);
  }
}
