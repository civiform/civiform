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
import javax.inject.Provider;
import models.AccountModel;
import org.junit.Before;
import org.junit.Test;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import services.session.SessionTimeoutService;
import services.settings.SettingsManifest;

public class ValidAccountFilterTest extends WithApplication {

  private ProfileUtils profileUtils;
  private SettingsManifest settingsManifest;
  private ValidAccountFilter filter;
  private CiviFormProfile mockProfile;
  private CiviFormProfileData mockProfileData;
  private AccountModel mockAccount;
  private SessionTimeoutService sessionTimeoutService;
  private Clock clock;

  @Before
  public void setUp() {
    profileUtils = mock(ProfileUtils.class);
    settingsManifest = mock(SettingsManifest.class);
    sessionTimeoutService = mock(SessionTimeoutService.class);
    clock = mock(Clock.class);
    Provider<SettingsManifest> settingsManifestProvider = () -> this.settingsManifest;

    filter =
        new ValidAccountFilter(
            profileUtils, settingsManifestProvider, mat, clock, () -> sessionTimeoutService);

    mockProfile = mock(CiviFormProfile.class);
    mockProfileData = mock(CiviFormProfileData.class);
    mockAccount = mock(AccountModel.class);

    when(mockProfile.getProfileData()).thenReturn(mockProfileData);
    when(mockProfile.getAccount()).thenReturn(CompletableFuture.completedFuture(mockAccount));

    // Default setup for session timeout service
    when(sessionTimeoutService.isSessionTimedOut(any()))
        .thenReturn(CompletableFuture.completedFuture(false));
  }

  @Test
  public void testValidProfile_sessionTimeoutDisabled_noUpdatesLastActivityTime() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile)).thenReturn(true);
    when(settingsManifest.getSessionTimeoutEnabled()).thenReturn(false);
    when(settingsManifest.getSessionReplayProtectionEnabled()).thenReturn(false);

    Result result = executeFilter(request);

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

    Result result = executeFilter(request);

    verify(mockProfileData).updateLastActivityTime(clock);
    assertThat(result.status()).isEqualTo(200);
  }

  @Test
  public void testSessionTimeout_redirectsToLogout() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile)).thenReturn(true);
    when(settingsManifest.getSessionTimeoutEnabled()).thenReturn(true);
    when(settingsManifest.getSessionReplayProtectionEnabled()).thenReturn(false);
    when(sessionTimeoutService.isSessionTimedOut(mockProfile))
        .thenReturn(CompletableFuture.completedFuture(true));

    Result result = executeFilter(request);

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

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(303); // Redirect status
    assertThat(result.redirectLocation()).hasValue("/logout");
  }

  @Test
  public void testInvalidProfile_redirectsToLogout() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile)).thenReturn(false);

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(303);
    assertThat(result.redirectLocation()).hasValue("/logout");
  }

  @Test
  public void testAllowedEndpoint_bypassesFilter() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/playIndex");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile)).thenReturn(false);

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(200);
  }

  @Test
  public void testLogoutRequest_bypassesFilter() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/logout");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile)).thenReturn(false);

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(200);
  }

  @Test
  public void testNoProfile_bypassesFilter() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.empty());

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(200);
  }

  private Result executeFilter(Http.RequestBuilder request) throws Exception {
    EssentialAction action =
        filter.apply(
            EssentialAction.of(requestHeader -> Accumulator.done(play.mvc.Results.ok("Success"))));
    return action.apply(request.build()).run(mat).toCompletableFuture().get();
  }
}
