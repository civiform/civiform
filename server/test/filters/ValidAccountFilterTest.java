package filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileUtils;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import models.AccountModel;
import org.junit.Before;
import org.junit.Test;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import services.settings.SettingsManifest;

public class ValidAccountFilterTest extends WithApplication {

  private ProfileUtils profileUtils;
  private SettingsManifest settingsManifest;
  private ValidAccountFilter filter;
  private CiviFormProfile mockProfile;
  private CiviFormProfileData mockProfileData;
  private AccountModel mockAccount;

  @Before
  public void setUp() {
    profileUtils = mock(ProfileUtils.class);
    settingsManifest = mock(SettingsManifest.class);
    filter =
        new ValidAccountFilter(profileUtils, () -> settingsManifest, mat, instanceOf(Clock.class));

    mockProfile = mock(CiviFormProfile.class);
    mockProfileData = mock(CiviFormProfileData.class);
    mockAccount = mock(AccountModel.class);

    when(mockProfile.getProfileData()).thenReturn(mockProfileData);
    when(mockProfile.getAccount()).thenReturn(CompletableFuture.completedFuture(mockAccount));
  }

  @Test
  public void testValidProfile_sessionTimeoutDisabled_noUpdatesLastActivityTime() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile)).thenReturn(true);
    when(settingsManifest.getSessionTimeoutEnabled()).thenReturn(false);
    when(settingsManifest.getSessionReplayProtectionEnabled()).thenReturn(false);

    EssentialAction action =
        filter.apply(
            EssentialAction.of(requestHeader -> Accumulator.done(play.mvc.Results.ok("Success"))));

    Result result = action.apply(request.build()).run(mat).toCompletableFuture().get();

    // when session timeout is disabled, we don't update last activity time
    verify(mockProfileData, never()).updateLastActivityTime(any());
    assertThat(result.status()).isEqualTo(200);
  }

  @Test
  public void testValidProfile_sessionTimeoutEnabled_updatesLastActivityTime() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile)).thenReturn(true);
    when(settingsManifest.getSessionTimeoutEnabled()).thenReturn(true);
    when(settingsManifest.getSessionReplayProtectionEnabled()).thenReturn(false);

    long currentTime = System.currentTimeMillis();
    long lastActivityTime = currentTime - (1 * 60 * 1000);
    when(mockProfileData.getLastActivityTime(any())).thenReturn(lastActivityTime);
    when(mockProfile.getSessionStartTime())
        .thenReturn(CompletableFuture.completedFuture(Optional.of(lastActivityTime)));

    EssentialAction action =
        filter.apply(
            EssentialAction.of(requestHeader -> Accumulator.done(play.mvc.Results.ok("Success"))));

    Result result = action.apply(request.build()).run(mat).toCompletableFuture().get();

    verify(mockProfileData).updateLastActivityTime(any());
    assertThat(result.status()).isEqualTo(200);
  }

  @Test
  public void testInvalidProfile_redirectsToLogout() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile)).thenReturn(false);

    EssentialAction action =
        filter.apply(
            EssentialAction.of(requestHeader -> Accumulator.done(play.mvc.Results.ok("Success"))));

    Result result = action.apply(request.build()).run(mat).toCompletableFuture().get();

    assertThat(result.status()).isEqualTo(303);
    assertThat(result.redirectLocation()).hasValue("/logout");
  }

  @Test
  public void testInactivityTimeout_sessionTimeoutEnabled_redirectsToLogout() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile)).thenReturn(true);
    when(settingsManifest.getSessionTimeoutEnabled()).thenReturn(true);
    when(settingsManifest.getSessionReplayProtectionEnabled()).thenReturn(false);
    when(settingsManifest.getSessionInactivityTimeoutMinutes()).thenReturn(Optional.of(30));

    long currentTime = System.currentTimeMillis();
    long lastActivityTime = currentTime - (31 * 60 * 1000);
    when(mockProfileData.getLastActivityTime(any())).thenReturn(lastActivityTime);
    when(mockProfile.getSessionStartTime())
        .thenReturn(CompletableFuture.completedFuture(Optional.of(lastActivityTime)));

    EssentialAction action =
        filter.apply(
            EssentialAction.of(requestHeader -> Accumulator.done(play.mvc.Results.ok("Success"))));

    Result result = action.apply(request.build()).run(mat).toCompletableFuture().get();

    assertThat(result.status()).isEqualTo(303);
    assertThat(result.redirectLocation()).hasValue("/logout");
  }

  @Test
  public void testSessionDurationExceeded_sessionTimeoutEnabled_redirectsToLogout()
      throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile)).thenReturn(true);
    when(settingsManifest.getSessionTimeoutEnabled()).thenReturn(true);
    when(settingsManifest.getSessionReplayProtectionEnabled()).thenReturn(false);
    when(settingsManifest.getSessionInactivityTimeoutMinutes()).thenReturn(Optional.of(30));
    when(settingsManifest.getMaximumSessionDurationMinutes()).thenReturn(Optional.of(480));

    long currentTime = System.currentTimeMillis();
    long sessionStartTime = currentTime - (481 * 60 * 1000);
    long lastActivityTime = currentTime - (5 * 60 * 1000);

    when(mockProfileData.getLastActivityTime(any())).thenReturn(lastActivityTime);
    when(mockProfile.getSessionStartTime())
        .thenReturn(CompletableFuture.completedFuture(Optional.of(sessionStartTime)));

    EssentialAction action =
        filter.apply(
            EssentialAction.of(requestHeader -> Accumulator.done(play.mvc.Results.ok("Success"))));

    Result result = action.apply(request.build()).run(mat).toCompletableFuture().get();

    assertThat(result.status()).isEqualTo(303);
    assertThat(result.redirectLocation()).hasValue("/logout");
  }

  @Test
  public void testInvalidSession_sessionTimeoutDisabled_redirectsToLogout() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile)).thenReturn(true);
    when(settingsManifest.getSessionTimeoutEnabled()).thenReturn(false);
    when(settingsManifest.getSessionReplayProtectionEnabled()).thenReturn(true);

    // Session ID not found in active sessions
    when(mockAccount.getActiveSession(anyString())).thenReturn(Optional.empty());
    when(mockProfileData.getSessionId()).thenReturn("session123");

    EssentialAction action =
        filter.apply(
            EssentialAction.of(requestHeader -> Accumulator.done(play.mvc.Results.ok("Success"))));

    Result result = action.apply(request.build()).run(mat).toCompletableFuture().get();

    assertThat(result.status()).isEqualTo(303); // Redirect status
    assertThat(result.redirectLocation()).hasValue("/logout");
  }

  @Test
  public void testAllowedEndpoint_bypassesFilter() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/playIndex");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile)).thenReturn(false);

    EssentialAction action =
        filter.apply(
            EssentialAction.of(requestHeader -> Accumulator.done(play.mvc.Results.ok("Success"))));

    Result result = action.apply(request.build()).run(mat).toCompletableFuture().get();

    assertThat(result.status()).isEqualTo(200);
  }

  @Test
  public void testLogoutRequest_bypassesFilter() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/logout");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile)).thenReturn(false);

    EssentialAction action =
        filter.apply(
            EssentialAction.of(requestHeader -> Accumulator.done(play.mvc.Results.ok("Success"))));

    Result result = action.apply(request.build()).run(mat).toCompletableFuture().get();

    assertThat(result.status()).isEqualTo(200);
  }

  @Test
  public void testNoProfile_bypassesFilter() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.empty());

    EssentialAction action =
        filter.apply(
            EssentialAction.of(requestHeader -> Accumulator.done(play.mvc.Results.ok("Success"))));

    Result result = action.apply(request.build()).run(mat).toCompletableFuture().get();

    assertThat(result.status()).isEqualTo(200);
  }
}
