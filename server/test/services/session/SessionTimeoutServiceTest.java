package services.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import services.settings.SettingsManifest;

public class SessionTimeoutServiceTest {
  private static final long CURRENT_TIME = 1000000000L;
  private static final int INACTIVITY_TIMEOUT_MINUTES = 30;
  private static final int MAX_SESSION_DURATION_MINUTES = 600;
  private static final int INACTIVITY_WARNING_MINUTES = 5;
  private static final int DURATION_WARNING_MINUTES = 10;

  private SessionTimeoutService sessionTimeoutService;
  private Clock clock;
  private Provider<SettingsManifest> settingsManifest;
  private CiviFormProfile profile;
  private CiviFormProfileData profileData;
  private SettingsManifest settings;

  @Before
  public void setUp() {
    clock = Clock.fixed(Instant.ofEpochSecond(CURRENT_TIME), ZoneId.systemDefault());
    settings = mock(SettingsManifest.class);
    settingsManifest = () -> settings;
    profile = mock(CiviFormProfile.class);
    profileData = mock(CiviFormProfileData.class);

    when(profile.getProfileData()).thenReturn(profileData);
    when(settings.getSessionInactivityTimeoutMinutes())
        .thenReturn(Optional.of(INACTIVITY_TIMEOUT_MINUTES));
    when(settings.getMaximumSessionDurationMinutes())
        .thenReturn(Optional.of(MAX_SESSION_DURATION_MINUTES));
    when(settings.getSessionInactivityWarningThresholdMinutes())
        .thenReturn(Optional.of(INACTIVITY_WARNING_MINUTES));
    when(settings.getSessionDurationWarningThresholdMinutes())
        .thenReturn(Optional.of(DURATION_WARNING_MINUTES));

    sessionTimeoutService = new SessionTimeoutService(settingsManifest, clock);
  }

  private long getTimeMinutesAgo(int minutes) {
    return CURRENT_TIME * 1000L - minutes * 60L * 1000L;
  }

  @Test
  public void isSessionTimedOut_noTimeout() {
    long lastActivityTime = getTimeMinutesAgo(10);
    when(profileData.getLastActivityTime(clock)).thenReturn(lastActivityTime);
    when(profileData.getSessionStartTime(clock)).thenReturn(lastActivityTime);

    boolean result = sessionTimeoutService.isSessionTimedOut(profile);

    assertThat(result).isFalse();
  }

  @Test
  public void isSessionTimedOut_inactivityTimeout() {
    long lastActivityTime = getTimeMinutesAgo(INACTIVITY_TIMEOUT_MINUTES + 1);
    when(profileData.getLastActivityTime(clock)).thenReturn(lastActivityTime);

    boolean result = sessionTimeoutService.isSessionTimedOut(profile);

    assertThat(result).isTrue();
  }

  @Test
  public void isSessionTimedOut_totalDurationTimeout() {
    long sessionStartTime = getTimeMinutesAgo(MAX_SESSION_DURATION_MINUTES + 1);
    when(profileData.getLastActivityTime(clock))
        .thenReturn(CURRENT_TIME * 1000 - 10 * 60 * 1000); // Recent activity
    when(profileData.getSessionStartTime(clock)).thenReturn(sessionStartTime);

    boolean result = sessionTimeoutService.isSessionTimedOut(profile);

    assertThat(result).isTrue();
  }

  @Test
  public void calculateTimeoutData_allValuesPresent() {
    long sessionStartTime = getTimeMinutesAgo(30);
    long lastActivityTime = getTimeMinutesAgo(10);
    when(profileData.getSessionStartTime(clock)).thenReturn(sessionStartTime);
    when(profileData.getLastActivityTime(clock)).thenReturn(lastActivityTime);

    SessionTimeoutService.TimeoutData timeoutData =
        sessionTimeoutService.calculateTimeoutData(profile);

    assertThat(timeoutData.inactivityTimeout())
        .isEqualTo(lastActivityTime / 1000 + INACTIVITY_TIMEOUT_MINUTES * 60);
    assertThat(timeoutData.totalTimeout())
        .isEqualTo(sessionStartTime / 1000 + MAX_SESSION_DURATION_MINUTES * 60);
    assertThat(timeoutData.inactivityWarning())
        .isEqualTo(
            lastActivityTime / 1000
                + (INACTIVITY_TIMEOUT_MINUTES - INACTIVITY_WARNING_MINUTES) * 60);
    assertThat(timeoutData.totalWarning())
        .isEqualTo(
            sessionStartTime / 1000
                + (MAX_SESSION_DURATION_MINUTES - DURATION_WARNING_MINUTES) * 60);
    assertThat(timeoutData.currentTime()).isEqualTo(CURRENT_TIME);
  }

  @Test
  public void calculateTimeoutData_noSessionStartTime_usesCurrentTime() {
    when(profileData.getSessionStartTime(clock)).thenReturn(CURRENT_TIME * 1000L);
    when(profileData.getLastActivityTime(clock)).thenReturn(getTimeMinutesAgo(10));

    SessionTimeoutService.TimeoutData timeoutData =
        sessionTimeoutService.calculateTimeoutData(profile);

    assertThat(timeoutData.totalTimeout())
        .isEqualTo(CURRENT_TIME + MAX_SESSION_DURATION_MINUTES * 60);
  }

  @Test
  public void missingInactivityTimeout_throwsException() {
    when(settings.getSessionInactivityTimeoutMinutes()).thenReturn(Optional.empty());

    assertThatThrownBy(() -> sessionTimeoutService.isSessionTimedOut(profile))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  public void missingMaxSessionDuration_throwsException() {
    when(settings.getMaximumSessionDurationMinutes()).thenReturn(Optional.empty());
    when(profileData.getLastActivityTime(clock)).thenReturn(getTimeMinutesAgo(10));
    assertThatThrownBy(() -> sessionTimeoutService.isSessionTimedOut(profile))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  public void missingSessionDurationWarningThreshold_throwsException() {
    when(settings.getSessionDurationWarningThresholdMinutes()).thenReturn(Optional.empty());
    long lastActivityTime = getTimeMinutesAgo(10);
    when(profileData.getLastActivityTime(clock)).thenReturn(lastActivityTime);
    when(profileData.getSessionStartTime(clock)).thenReturn(lastActivityTime);

    assertThatThrownBy(() -> sessionTimeoutService.calculateTimeoutData(profile))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  public void missingSessionInactivityWarningThreshold_throwsException() {
    when(settings.getSessionInactivityWarningThresholdMinutes()).thenReturn(Optional.empty());
    long lastActivityTime = getTimeMinutesAgo(10);
    when(profileData.getLastActivityTime(clock)).thenReturn(lastActivityTime);
    when(profileData.getSessionStartTime(clock)).thenReturn(lastActivityTime);

    assertThatThrownBy(() -> sessionTimeoutService.calculateTimeoutData(profile))
        .isInstanceOf(NoSuchElementException.class);
  }
}
