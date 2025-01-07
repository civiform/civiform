package filters;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.typesafe.config.Config;
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
  private final Config config;

  @Inject
  public ValidAccountFilter(ProfileUtils profileUtils, Config config) {
    this.profileUtils = checkNotNull(profileUtils);
    this.config = checkNotNull(config);
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request -> {
          Optional<CiviFormProfile> profile = profileUtils.optionalCurrentUserProfile(request);
          if (profile.isPresent() && shouldLogoutUser(profile.get())) {
            // The cookie is present but the profile or session is not valid, redirect to logout and
            // clear the cookie.
            if (!allowedEndpoint(request)) {
              return Accumulator.done(
                  Results.redirect(org.pac4j.play.routes.LogoutController.logout()));
            }
          }

          return next.apply(request);
        });
  }

  private boolean shouldLogoutUser(CiviFormProfile profile) {
    return !profileUtils.validCiviFormProfile(profile) || !isValidSession(profile);
  }

  private boolean isValidSession(CiviFormProfile profile) {
    if (config.getBoolean("session_replay_protection_enabled")) {
      profile
          .getAccount()
          .thenApply(
              account ->
                  account.getActiveSession(profile.getProfileData().getSessionId()).isPresent())
          .join();
    }
    return true;
  }

  /**
   * Return true if the endpoint does not require a profile. Logout url is necessary here to avoid
   * infinite redirect.
   *
   * <p>NOTE: You might think we'd also want an OptionalProfileRoutes check here. However, this is
   * currently only called after checking if a profile is present and valid. If the profile isn't
   * present, we never make it here anyway. If the profile is invalid, we don't want to allow
   * hitting those endpoints with an invalid profile, so we don't add that check here.
   */
  private boolean allowedEndpoint(Http.RequestHeader requestHeader) {
    return NonUserRoutes.anyMatch(requestHeader) || isLogoutRequest(requestHeader.uri());
  }

  /** Return true if the request is to the logout endpoint. */
  private boolean isLogoutRequest(String uri) {
    return uri.startsWith(org.pac4j.play.routes.LogoutController.logout().url());
  }
}
