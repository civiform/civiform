package services.session;

import auth.CiviFormProfile;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import javax.inject.Provider;
import services.settings.SettingsManifest;

/** Service responsible for managing session timeout logic in CiviForm. */
public final class SessionTimeoutService {
  private final Provider<SettingsManifest> settingsManifest;
  private final Clock clock;

  @Inject
  public SessionTimeoutService(Provider<SettingsManifest> settingsManifest, Clock clock) {
    this.settingsManifest = settingsManifest;
    this.clock = clock;
  }

  /**
   * Determines if the session has timed out due to inactivity or exceeding the maximum session
   * duration.
   *
   * @param profile the user's profile
   * @return true if the session has timed out, false otherwise
   */
  public boolean isSessionTimedOut(CiviFormProfile profile) {
    return isSessionTimedOutDueToInactivity(profile)
        || isSessionTimedOutDueToSessionLength(profile);
  }

  private boolean isSessionTimedOutDueToInactivity(CiviFormProfile profile) {
    long currentTimeInMillis = clock.millis();
    long lastActivityTimeInMillis = profile.getProfileData().getLastActivityTime(clock);

    long inactivityTimeoutInMillis = getInactivityTimeoutMillis();

    return (currentTimeInMillis - lastActivityTimeInMillis) > inactivityTimeoutInMillis;
  }

  /**
   * Calculates the timeout data for the user's session.
   *
   * @param profile the user's profile
   * @return a future that completes with the timeout data for the user's session
   */
  public TimeoutData calculateTimeoutData(CiviFormProfile profile) {
    long currentTime = clock.instant().getEpochSecond();

    long sessionStartTimeInMillis = profile.getProfileData().getSessionStartTime(clock);

    long sessionStartTimeInSeconds = sessionStartTimeInMillis / 1000;

    int inactivityMinutes = getSessionInactivityTimeoutMinutes();
    int totalLengthMinutes = getMaximumSessionDurationMinutes();
    int inactivityWarningMinutes =
        settingsManifest.get().getSessionInactivityWarningThresholdMinutes().orElseThrow();
    int durationWarningMinutes =
        settingsManifest.get().getSessionDurationWarningThresholdMinutes().orElseThrow();

    long lastActivityTimeInSeconds = profile.getProfileData().getLastActivityTime(clock) / 1000;
    return new TimeoutData(
        calculateTimeoutLimit(lastActivityTimeInSeconds, inactivityMinutes),
        calculateTimeoutLimit(sessionStartTimeInSeconds, totalLengthMinutes),
        calculateWarningTime(
            lastActivityTimeInSeconds, inactivityMinutes, inactivityWarningMinutes),
        calculateWarningTime(sessionStartTimeInSeconds, totalLengthMinutes, durationWarningMinutes),
        currentTime);
  }

  private long calculateTimeoutLimit(long startTimeInSeconds, int timeoutMinutes) {
    return Instant.ofEpochSecond(startTimeInSeconds)
        .plus(timeoutMinutes, ChronoUnit.MINUTES)
        .getEpochSecond();
  }

  private long calculateWarningTime(
      long startTimeInSeconds, int timeoutMinutes, int warningMinutes) {
    return Instant.ofEpochSecond(startTimeInSeconds)
        .plus(timeoutMinutes - warningMinutes, ChronoUnit.MINUTES)
        .getEpochSecond();
  }

  private boolean isSessionTimedOutDueToSessionLength(CiviFormProfile profile) {
    long currentTimeInMillis = clock.millis();
    long maxSessionDurationInMillis = getMaxSessionDurationMillis();
    long sessionStartTimeInMillis = profile.getProfileData().getSessionStartTime(clock);
    return (currentTimeInMillis - sessionStartTimeInMillis) > maxSessionDurationInMillis;
  }

  private long getInactivityTimeoutMillis() {
    return getSessionInactivityTimeoutMinutes() * 60L * 1000;
  }

  private int getSessionInactivityTimeoutMinutes() {
    return settingsManifest.get().getSessionInactivityTimeoutMinutes().orElseThrow();
  }

  private long getMaxSessionDurationMillis() {
    return getMaximumSessionDurationMinutes() * 60L * 1000;
  }

  private int getMaximumSessionDurationMinutes() {
    return settingsManifest.get().getMaximumSessionDurationMinutes().orElseThrow();
  }

  public record TimeoutData(
      long inactivityTimeout,
      long totalTimeout,
      long inactivityWarning,
      long totalWarning,
      long currentTime) {}
}
