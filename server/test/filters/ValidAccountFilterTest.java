package filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileUtils;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import models.AccountModel;
import org.apache.pekko.stream.testkit.NoMaterializer$;
import org.junit.Before;
import org.junit.Test;
import play.i18n.MessagesApi;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import services.settings.SettingsManifest;

public class ValidAccountFilterTest extends WithApplication {

  private ProfileUtils profileUtils;
  private SettingsManifest settingsManifest;
  private MessagesApi messagesApi;
  private ValidAccountFilter filter;
  private CiviFormProfile mockProfile;
  private CiviFormProfileData mockProfileData;
  private AccountModel mockAccount;

  @Before
  public void setUp() {
    profileUtils = mock(ProfileUtils.class);
    settingsManifest = mock(SettingsManifest.class);
    messagesApi = mock(MessagesApi.class);

    filter =
        new ValidAccountFilter(
            profileUtils, () -> settingsManifest, messagesApi, instanceOf(Clock.class));

    mockProfile = mock(CiviFormProfile.class);
    mockProfileData = mock(CiviFormProfileData.class);
    mockAccount = mock(AccountModel.class);

    when(mockProfile.getProfileData()).thenReturn(mockProfileData);
    when(mockProfile.getAccount()).thenReturn(CompletableFuture.completedFuture(mockAccount));

    play.i18n.Messages mockMessages = mock(play.i18n.Messages.class);
    when(messagesApi.preferred(any(Http.RequestHeader.class))).thenReturn(mockMessages);
    when(mockMessages.at(anyString())).thenReturn("Logout message");
  }

  @Test
  public void testValidProfile_UpdatesLastActivityTime() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile)).thenReturn(true);
    when(settingsManifest.getSessionTimeoutEnabled()).thenReturn(false);
    when(settingsManifest.getSessionReplayProtectionEnabled()).thenReturn(false);

    EssentialAction action =
        filter.apply(
            EssentialAction.of(requestHeader -> Accumulator.done(play.mvc.Results.ok("Success"))));

    Result result =
        action.apply(request.build()).run(NoMaterializer$.MODULE$).toCompletableFuture().get();

    verify(mockProfileData).updateLastActivityTime(any());
    assertThat(result.status()).isEqualTo(200);
  }

  @Test
  public void testInvalidProfile_RedirectsToLogout() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile)).thenReturn(false);

    EssentialAction action =
        filter.apply(
            EssentialAction.of(requestHeader -> Accumulator.done(play.mvc.Results.ok("Success"))));

    Result result =
        action.apply(request.build()).run(NoMaterializer$.MODULE$).toCompletableFuture().get();

    assertThat(result.status()).isEqualTo(303); // Redirect status
    assertThat(result.redirectLocation()).hasValue("/logout");
    assertThat(result.flash().get("error")).hasValue("Logout message");
  }

  @Test
  public void testInactivityTimeout_RedirectsToLogout() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile)).thenReturn(true);
    when(settingsManifest.getSessionTimeoutEnabled()).thenReturn(true);
    when(settingsManifest.getSessionReplayProtectionEnabled()).thenReturn(false);
    when(settingsManifest.getSessionInactivityTimeoutMinutes()).thenReturn(Optional.of(30));

    long currentTime = System.currentTimeMillis();
    long lastActivityTime = currentTime - (31 * 60 * 1000); // 31 minutes ago
    when(mockProfileData.getLastActivityTime(any())).thenReturn(lastActivityTime);
    when(mockProfile.getSessionStartTime()).thenReturn(Optional.of(lastActivityTime));

    EssentialAction action =
        filter.apply(
            EssentialAction.of(requestHeader -> Accumulator.done(play.mvc.Results.ok("Success"))));

    Result result =
        action.apply(request.build()).run(NoMaterializer$.MODULE$).toCompletableFuture().get();

    assertThat(result.status()).isEqualTo(303); // Redirect status
    assertThat(result.redirectLocation()).hasValue("/logout");
    assertThat(result.flash().get("error")).hasValue("Logout message");
  }

  @Test
  public void testSessionDurationExceeded_RedirectsToLogout() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile)).thenReturn(true);
    when(settingsManifest.getSessionTimeoutEnabled()).thenReturn(true);
    when(settingsManifest.getSessionReplayProtectionEnabled()).thenReturn(false);
    when(settingsManifest.getSessionInactivityTimeoutMinutes()).thenReturn(Optional.of(30));
    when(settingsManifest.getMaximumSessionDurationMinutes()).thenReturn(Optional.of(480));

    long currentTime = System.currentTimeMillis();
    long sessionStartTime = currentTime - (481 * 60 * 1000); // 481 minutes ago (> 8 hours)
    long lastActivityTime =
        currentTime - (5 * 60 * 1000); // 5 minutes ago (within inactivity timeout)

    when(mockProfileData.getLastActivityTime(any())).thenReturn(lastActivityTime);
    when(mockProfile.getSessionStartTime()).thenReturn(Optional.of(sessionStartTime));

    EssentialAction action =
        filter.apply(
            EssentialAction.of(requestHeader -> Accumulator.done(play.mvc.Results.ok("Success"))));

    Result result =
        action.apply(request.build()).run(NoMaterializer$.MODULE$).toCompletableFuture().get();

    assertThat(result.status()).isEqualTo(303); // Redirect status
    assertThat(result.redirectLocation()).hasValue("/logout");
    assertThat(result.flash().get("error")).hasValue("Logout message");
  }

  @Test
  public void testInvalidSession_RedirectsToLogout() throws Exception {
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

    Result result =
        action.apply(request.build()).run(NoMaterializer$.MODULE$).toCompletableFuture().get();

    assertThat(result.status()).isEqualTo(303); // Redirect status
    assertThat(result.redirectLocation()).hasValue("/logout");
    assertThat(result.flash().get("error")).hasValue("Logout message");
  }

  @Test
  public void testAllowedEndpoint_BypassesFilter() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/playIndex");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile)).thenReturn(false);

    EssentialAction action =
        filter.apply(
            EssentialAction.of(requestHeader -> Accumulator.done(play.mvc.Results.ok("Success"))));

    Result result =
        action.apply(request.build()).run(NoMaterializer$.MODULE$).toCompletableFuture().get();

    assertThat(result.status()).isEqualTo(200);
  }

  @Test
  public void testLogoutRequest_BypassesFilter() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/logout");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile)).thenReturn(false);

    EssentialAction action =
        filter.apply(
            EssentialAction.of(requestHeader -> Accumulator.done(play.mvc.Results.ok("Success"))));

    Result result =
        action.apply(request.build()).run(NoMaterializer$.MODULE$).toCompletableFuture().get();

    assertThat(result.status()).isEqualTo(200);
  }

  @Test
  public void testNoProfile_BypassesFilter() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.empty());

    EssentialAction action =
        filter.apply(
            EssentialAction.of(requestHeader -> Accumulator.done(play.mvc.Results.ok("Success"))));

    Result result =
        action.apply(request.build()).run(NoMaterializer$.MODULE$).toCompletableFuture().get();

    assertThat(result.status()).isEqualTo(200);
  }
}
