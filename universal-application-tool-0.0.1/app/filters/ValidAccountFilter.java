package filters;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import controllers.routes;
import java.util.Optional;
import javax.inject.Inject;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
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
            if (!allowedEndpoint(request.uri())) {
              return Accumulator.done(
                  Results.redirect(org.pac4j.play.routes.LogoutController.logout()));
            }
          }

          // Check to see if the account is the unconfirmed email placeholder account from Seattle
          // IDCS. If it is,
          // redirect to a custom support page for that purpose.
          if (profile.isPresent()
              && profileUtils.accountIsIdcsPlaceholder(profile.get())
              // Guard against a redirect loop
              && !request
                  .uri()
                  .startsWith(routes.SupportController.handleUnconfirmedIdcsEmail().path())) {
            return Accumulator.done(
                Results.redirect(
                    controllers.routes.SupportController.handleUnconfirmedIdcsEmail()));
          }

          return next.apply(request);
        });
  }

  /**
   * Return true if the endpoint does not require a profile. Logout url is necessary here to avoid
   * infinite redirect.
   */
  private boolean allowedEndpoint(String uri) {
    if (uri.startsWith("/assets")) {
      return true;
    }
    if (uri.startsWith("/dev")) {
      return true;
    }
    String logoutUrl = org.pac4j.play.routes.LogoutController.logout().url();
    if (uri.startsWith(logoutUrl)) {
      return true;
    }
    return false;
  }
}
