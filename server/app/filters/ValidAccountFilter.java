package filters;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import java.time.Clock;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import play.i18n.MessagesApi;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Http;
import play.mvc.Results;
import services.MessageKey;
import services.settings.SettingsManifest;

/**
 * A filter to ensure the account referenced in the browser cookie is valid. This should only matter
 * when the account is deleted from the database which almost will never happen in prod database.
 */
public class ValidAccountFilter extends EssentialFilter {

  private static final int DEFAULT_INACTIVITY_TIMEOUT_MINUTES = 30;
  private static final int DEFAULT_MAX_SESSION_DURATION_MINUTES = 480;

  private final ProfileUtils profileUtils;
  private final Provider<SettingsManifest> settingsManifest;
  private final MessagesApi messagesApi;
  private final Clock clock;

  public enum LogoutReason {
    INVALID_PROFILE(MessageKey.TOAST_LOGOUT_INVALID_SESSION.getKeyName()),
    INVALID_SESSION(MessageKey.TOAST_LOGOUT_INVALID_SESSION.getKeyName()),
    INACTIVITY_TIMEOUT(MessageKey.TOAST_LOGOUT_TIMEOUT_INACTIVITY.getKeyName()),
    DURATION_EXCEEDED(MessageKey.TOAST_LOGOUT_TIMEOUT_DURATION.getKeyName());

    private final String messageKey;

    LogoutReason(String messageKey) {
      this.messageKey = messageKey;
    }

    public String getMessageKey() {
      return messageKey;
    }
  }

  @Inject
  public ValidAccountFilter(
      ProfileUtils profileUtils,
      Provider<SettingsManifest> settingsManifest,
      MessagesApi messagesApi,
      Clock clock) {
    this.profileUtils = checkNotNull(profileUtils);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.messagesApi = checkNotNull(messagesApi);
    this.clock = checkNotNull(clock);
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request -> {
          Optional<CiviFormProfile> profile = profileUtils.optionalCurrentUserProfile(request);

          Optional<LogoutReason> logoutReason = getLogoutReason(profile);
          if (logoutReason.isPresent() && !allowedEndpoint(request)) {
            String message = messagesApi.preferred(request).at(logoutReason.get().getMessageKey());

            return Accumulator.done(
                Results.redirect(org.pac4j.play.routes.LogoutController.logout())
                    .flashing("error", message));
          }

          profile.ifPresent(p -> p.getProfileData().updateLastActivityTime(clock));
          return next.apply(request);
        });
  }

  private Optional<LogoutReason> getLogoutReason(Optional<CiviFormProfile> maybeProfile) {
    if (maybeProfile.isEmpty()) {
      return Optional.empty();
    }

    CiviFormProfile profile = maybeProfile.get();

    // Check basic validity first
    if (!profileUtils.validCiviFormProfile(profile)) {
      return Optional.of(LogoutReason.INVALID_PROFILE);
    }

    if (!isValidSession(profile)) {
      return Optional.of(LogoutReason.INVALID_SESSION);
    }

    // Only check timeout conditions if the feature is enabled
    if (!settingsManifest.get().getSessionTimeoutEnabled()) {
      return Optional.empty();
    }

    long currentTime = clock.millis();
    long lastActivityTime = profile.getProfileData().getLastActivityTime(clock);
    long sessionStartTime = profile.getSessionStartTime().orElse(currentTime);

    // Get timeout values with defaults
    long inactivityTimeout =
        settingsManifest
                .get()
                .getSessionInactivityTimeoutMinutes()
                .orElse(DEFAULT_INACTIVITY_TIMEOUT_MINUTES)
            * 60L
            * 1000; // Default 30 minutes

    long maxSessionDuration =
        settingsManifest
                .get()
                .getMaximumSessionDurationMinutes()
                .orElse(DEFAULT_MAX_SESSION_DURATION_MINUTES)
            * 60L
            * 1000; // Default 8 hours

    if (currentTime - lastActivityTime > inactivityTimeout) {
      return Optional.of(LogoutReason.INACTIVITY_TIMEOUT);
    }

    if (currentTime - sessionStartTime > maxSessionDuration) {
      return Optional.of(LogoutReason.DURATION_EXCEEDED);
    }

    return Optional.empty();
  }

  private boolean isValidSession(CiviFormProfile profile) {
    if (settingsManifest.get().getSessionReplayProtectionEnabled()) {
      return profile
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
