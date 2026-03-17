package filters;

import static com.google.common.base.Preconditions.checkNotNull;
import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;
import static play.mvc.Results.redirect;

import auth.CiviFormProfile;
import auth.GuestClient;
import auth.ProfileUtils;
import com.google.inject.Inject;
import controllers.routes;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.inject.Provider;
import org.apache.pekko.stream.Materializer;
import play.mvc.Filter;
import play.mvc.Http;
import play.mvc.Result;
import play.routing.Router;

/**
 * Ensures that user-facing requests have a CiviFormProfile by redirecting to create a guest session
 * then redirecting back to the original request.
 */
public final class CiviFormProfileFilter extends Filter {
  private final ProfileUtils profileUtils;
  private final Provider<Router> routerProvider;

  @Inject
  public CiviFormProfileFilter(
      Materializer mat, ProfileUtils profileUtils, Provider<Router> routerProvider) {
    super(mat);
    this.profileUtils = checkNotNull(profileUtils);
    this.routerProvider = checkNotNull(routerProvider);
  }

  /**
   * Determines whether to redirect this request to create a guest profile. Returns true if:
   *
   * <ul>
   *   <li>The request is for a user-facing route
   *   <li>The request is not for the homepage (/ or /programs) or error page (/error)
   *   <li>The request is for a route that exists (won't result in a 404)
   *   <li>The request uses the `GET` or `HEAD` method (POST cannot be redirected back to the
   *       original URI)
   *   <li>The session associated with the request does not contain a pac4j user profile
   * </ul>
   */
  private boolean shouldRedirect(Http.RequestHeader requestHeader) {
    return NonUserRoutes.noneMatch(requestHeader)
        && OptionalProfileRoutes.noneMatch(requestHeader)
        && routerProvider.get().route(requestHeader).isPresent()
        && !requestHeader.path().startsWith("/callback")
        // TODO(#8504) extend to all HTTP methods
        && (requestHeader.method().equals("GET") || requestHeader.method().equals("HEAD"));
  }

  @Override
  public CompletionStage<Result> apply(
      Function<Http.RequestHeader, CompletionStage<Result>> nextFilter,
      Http.RequestHeader requestHeader) {
    if (shouldRedirect(requestHeader)) {
      // Only get the user profile if the previous conditions pass
      Optional<CiviFormProfile> currentUserProfile =
          profileUtils.optionalCurrentUserProfile(requestHeader);
      if (currentUserProfile.isEmpty()) {
        // Directly invoke the callback of the GuestClient, which creates a profile.
        // Then redirect the user to the page they were trying to reach.
        return CompletableFuture.completedFuture(
            redirect(routes.CallbackController.callback(GuestClient.CLIENT_NAME).url())
                .withSession(
                    requestHeader.session().adding(REDIRECT_TO_SESSION_KEY, requestHeader.uri())));
      } else {
        // Attach the profile so downstream actions don't need to re-fetch it
        requestHeader =
            requestHeader.addAttr(ProfileUtils.CURRENT_USER_PROFILE, currentUserProfile.get());
      }
    }

    // Do nothing
    return nextFilter.apply(requestHeader);
  }
}
