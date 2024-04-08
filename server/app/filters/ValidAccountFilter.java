package filters;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import java.util.Optional;
import javax.inject.Inject;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Http;
import play.mvc.Results;

/**
 * A filter to ensure the account referenced in the browser cookie is valid. This should only matter
 * when the account is deleted from the database which almost will never happen in prod database.
 */
public class ValidAccountFilter extends EssentialFilter {
  private final ProfileUtils profileUtils;

  @Inject
  public ValidAccountFilter(ProfileUtils profileUtils) {
    this.profileUtils = checkNotNull(profileUtils);
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request -> {
          Optional<CiviFormProfile> profile = profileUtils.currentUserProfile(request);
          if (profile.isPresent() && !profileUtils.validCiviFormProfile(profile.get())) {
            // The cookie is present but the profile is not valid, redirect to logout and clear the
            // cookie.
            if (!allowedEndpoint(request)) {
              return Accumulator.done(
                  Results.redirect(org.pac4j.play.routes.LogoutController.logout()));
            }
          }

          return next.apply(request);
        });
  }

  /**
   * Return true if the endpoint does not require a profile. Logout url is necessary here to avoid
   * infinite redirect.
   */
  private boolean allowedEndpoint(Http.RequestHeader requestHeader) {
    return NonUserRoutePrefixes.anyMatch(requestHeader) || isLogoutRequest(requestHeader.uri());
  }

  /** Return true if the request is to the logout endpoint. */
  private boolean isLogoutRequest(String uri) {
    return uri.startsWith(org.pac4j.play.routes.LogoutController.logout().url());
  }
}
