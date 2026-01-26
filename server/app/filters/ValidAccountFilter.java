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
import repository.DatabaseExecutionContext;
import services.session.SessionTimeoutService;
import services.settings.SettingsManifest;

/**
 * This filter validates the user's account and session. The user is logged out if the account no
 * longer exists (which should only happen if an account is created and then deleted from the
 * database), the session is invalid, or the session has timed out. If the account and session is
 * valid and the session timeout feature flag is enabled, the user's last activity time is updated.
 */
public class ValidAccountFilter extends EssentialFilter {

  private final ProfileUtils profileUtils;
  private final Provider<SettingsManifest> settingsManifest;
  private final Materializer materializer;
  private final Clock clock;
  private final Provider<SessionTimeoutService> sessionTimeoutService;
  private final Provider<DatabaseExecutionContext> databaseExecutionContext;

  @Inject
  public ValidAccountFilter(
      ProfileUtils profileUtils,
      Provider<SettingsManifest> settingsManifest,
      Materializer materializer,
      Clock clock,
      Provider<SessionTimeoutService> sessionTimeoutService,
      Provider<DatabaseExecutionContext> databaseExecutionContext) {
    this.profileUtils = checkNotNull(profileUtils);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.materializer = checkNotNull(materializer);
    this.clock = checkNotNull(clock);
    this.sessionTimeoutService = checkNotNull(sessionTimeoutService);
    this.databaseExecutionContext = checkNotNull(databaseExecutionContext);
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request -> {
          if (allowedEndpoint(request)) {
            return next.apply(request);
          }

          Optional<CiviFormProfile> profile = profileUtils.optionalCurrentUserProfile(request);

          if (profile.isEmpty()) {
            return next.apply(request);
          }

          CompletionStage<Accumulator<ByteString, Result>> futureAccumulator =
              shouldLogoutUser(profile.get(), request)
                  .thenApply(
                      shouldLogout -> {
                        if (shouldLogout) {
                          return Accumulator.done(
                              Results.redirect(org.pac4j.play.routes.LogoutController.logout()));
                        } else {
                          if (settingsManifest.get().getSessionTimeoutEnabled(request)) {
                            profile.get().getProfileData().updateLastActivityTime(clock);
                          }
                          return next.apply(request);
                        }
                      });

          return Accumulator.flatten(futureAccumulator, materializer);
        });
  }

  private CompletionStage<Boolean> shouldLogoutUser(
      CiviFormProfile profile, Http.RequestHeader request) {
    return profileUtils
        .validCiviFormProfile(profile)
        .thenComposeAsync(
            profileValid -> {
              if (!profileValid) {
                return CompletableFuture.completedFuture(false);
              }
              return isValidSession(profile);
            },
            databaseExecutionContext.get())
        .thenComposeAsync(
            isValidProfileAndSession -> {
              if (!isValidProfileAndSession) {
                // Log out if either profile or session was invalid
                return CompletableFuture.completedFuture(true);
              }
              // If flag is disabled, keep them logged in if they have a valid session
              if (!settingsManifest.get().getSessionTimeoutEnabled(request)) {
                return CompletableFuture.completedFuture(false);
              }
              // Otherwise, let the SessionTimeoutService decide
              return sessionTimeoutService.get().isSessionTimedOut(profile);
            },
            databaseExecutionContext.get());
  }

  private CompletionStage<Boolean> isValidSession(CiviFormProfile profile) {
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
