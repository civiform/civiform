package filters;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Provider;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.util.ByteString;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import services.session.SessionTimeoutService;
import services.settings.SettingsManifest;

/**
 * A filter to ensure the account referenced in the browser cookie is valid. This should only matter
 * when the account is deleted from the database which almost will never happen in prod database.
 */
public class ValidAccountFilter extends EssentialFilter {

  private final ProfileUtils profileUtils;
  private final Provider<SettingsManifest> settingsManifest;
  private final Materializer materializer;
  private final Clock clock;
  private final Provider<SessionTimeoutService> sessionTimeoutService;

  @Inject
  public ValidAccountFilter(
      ProfileUtils profileUtils,
      Provider<SettingsManifest> settingsManifest,
      Materializer materializer,
      Clock clock,
      Provider<SessionTimeoutService> sessionTimeoutService) {
    this.profileUtils = checkNotNull(profileUtils);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.materializer = checkNotNull(materializer);
    this.clock = checkNotNull(clock);
    this.sessionTimeoutService = sessionTimeoutService;
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request -> {
          Optional<CiviFormProfile> profile = profileUtils.optionalCurrentUserProfile(request);

          if (profile.isEmpty() || allowedEndpoint(request)) {
            return next.apply(request);
          }

          CompletionStage<Accumulator<ByteString, Result>> futureAccumulator =
              shouldLogoutUser(profile.get())
                  .thenApply(
                      shouldLogout -> {
                        if (shouldLogout) {
                          return Accumulator.done(
                              Results.redirect(org.pac4j.play.routes.LogoutController.logout()));
                        } else {
                          if (settingsManifest.get().getSessionTimeoutEnabled()) {
                            profile.get().getProfileData().updateLastActivityTime(clock);
                          }
                          return next.apply(request);
                        }
                      });

          return Accumulator.flatten(futureAccumulator, materializer);
        });
  }

  private CompletableFuture<Boolean> shouldLogoutUser(CiviFormProfile profile) {
    if (!profileUtils.validCiviFormProfile(profile)) {
      return CompletableFuture.completedFuture(true);
    }

    return isValidSession(profile)
        .thenCompose(
            isValid -> {
              if (!isValid) {
                return CompletableFuture.completedFuture(true);
              }

              if (!settingsManifest.get().getSessionTimeoutEnabled()) {
                return CompletableFuture.completedFuture(false);
              }

              return sessionTimeoutService.get().isSessionTimedOut(profile);
            });
  }

  private CompletableFuture<Boolean> isValidSession(CiviFormProfile profile) {
    if (settingsManifest.get().getSessionReplayProtectionEnabled()) {
      return profile
          .getAccount()
          .thenApply(
              account ->
                  account.getActiveSession(profile.getProfileData().getSessionId()).isPresent());
    }
    return CompletableFuture.completedFuture(true);
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
