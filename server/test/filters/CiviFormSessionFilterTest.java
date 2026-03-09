package filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import models.AccountModel;
import models.SessionDetails;
import org.junit.Before;
import org.junit.Test;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.Http;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.test.WithApplication;
import repository.DatabaseExecutionContext;
import services.settings.SettingsManifest;

public class ValidAccountFilterTest extends WithApplication {
  private static final String TIMEOUT_COOKIE_NAME = "session_timeout_data";
  private static final long CURRENT_TIME = 1000000000L;
  private static final String SESSION_ID = "test-session-id";

  private ProfileUtils profileUtils;
  private SettingsManifest settingsManifest;
  private ValidAccountFilter filter;
  private SettingsManifest settingsManifest;
  private SessionTimeoutService sessionTimeoutService;
  private CiviFormProfile mockProfile;
  private CiviFormProfileData mockProfileData;
  private AccountModel mockAccount;
  private Clock clock;

  private final SessionTimeoutService.TimeoutData defaultTimeoutData =
      new SessionTimeoutService.TimeoutData(
          CURRENT_TIME + (30 * 60),
          CURRENT_TIME + (10 * 60 * 60),
          CURRENT_TIME + (25 * 60),
          CURRENT_TIME + (9 * 60 * 60 + 50 * 60),
          CURRENT_TIME);

  @Before
  public void setUp() {
    profileUtils = mock(ProfileUtils.class);
    settingsManifest = mock(SettingsManifest.class);

    filter =
        new CiviFormSessionFilter(
            profileUtils,
            () -> settingsManifest, // Provider<SettingsManifest>
            mat,
            () -> instanceOf(DatabaseExecutionContext.class) // Provider<DatabaseExecutionContext>
            );

    mockProfile = mock(CiviFormProfile.class);
    mockProfileData = mock(CiviFormProfileData.class);
    mockAccount = mock(AccountModel.class);
    SessionDetails sessionDetails = new SessionDetails();
    sessionDetails.setCreationTime(Instant.ofEpochMilli(CURRENT_TIME * 1000));

    when(mockProfile.getProfileData()).thenReturn(mockProfileData);
    when(mockProfile.getAccount()).thenReturn(CompletableFuture.completedFuture(mockAccount));
    when(mockProfileData.getSessionId()).thenReturn(SESSION_ID);
    when(mockAccount.getActiveSession(SESSION_ID)).thenReturn(Optional.of(sessionDetails));
    when(clock.millis()).thenReturn(CURRENT_TIME * 1000);
    when(sessionTimeoutService.calculateTimeoutData(eq(mockProfile), anyLong()))
        .thenReturn(defaultTimeoutData);
  }

  // --- Allowed endpoints ---

  @Test
  public void testAllowedEndpoint_bypassesFilter() throws Exception {
    RequestHeader request = fakeRequestBuilder().method("GET").uri("/playIndex").build();

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(200);
  }

  @Test
  public void testValidProfile_sessionTimeoutDisabled_noUpdatesLastActivityTime() throws Exception {
    RequestHeader request = fakeRequestBuilder().method("GET").uri("/programs/1").build();
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile))
        .thenReturn(CompletableFuture.completedFuture(true));
    when(settingsManifest.getSessionTimeoutEnabled(request)).thenReturn(false);
    when(settingsManifest.getSessionReplayProtectionEnabled()).thenReturn(false);

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(200);
    Optional<Http.Cookie> cookie = result.cookies().get(TIMEOUT_COOKIE_NAME);
    assertThat(cookie).isPresent();
    assertThat(cookie.get().maxAge().longValue()).isEqualTo(Duration.ZERO.toSeconds());
  }

  // --- Invalid account or session ---

  @Test
  public void testInvalidSession_sessionTimeoutDisabled_redirectsToLogout() throws Exception {
    RequestHeader request = fakeRequestBuilder().method("GET").uri("/programs/1").build();
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(mockProfile));
    when(profileUtils.validCiviFormProfile(mockProfile))
        .thenReturn(CompletableFuture.completedFuture(true));
    when(settingsManifest.getSessionTimeoutEnabled(request)).thenReturn(false);
    when(settingsManifest.getSessionReplayProtectionEnabled()).thenReturn(true);

    // Session ID not found in active sessions
    when(mockAccount.getActiveSession(anyString())).thenReturn(Optional.empty());
    when(mockProfileData.getSessionId()).thenReturn("session123");

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(303); // Redirect status
    assertThat(result.redirectLocation()).hasValue("/logout");
  }

  @Test
  public void testInvalidAccount_redirectsToLogout() throws Exception {
    RequestHeader request = fakeRequestBuilder().method("GET").uri("/programs/1").build();
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(mockProfile));
    when(mockProfile.getAccount())
        .thenReturn(
            CompletableFuture.failedFuture(
                new auth.AccountNonexistentException("account not found")));

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(303);
    assertThat(result.redirectLocation()).hasValue("/logout");
  }

  @Test
  public void testInvalidSession_redirectsToLogout() throws Exception {
    RequestHeader request = fakeRequestBuilder().method("GET").uri("/programs/1").build();
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(mockProfile));
    when(mockAccount.getActiveSession(SESSION_ID)).thenReturn(Optional.empty());

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(303);
    assertThat(result.redirectLocation()).hasValue("/logout");
  }

  // --- Timeout disabled ---

  @Test
  public void testTimeoutDisabled_clearsCookie() throws Exception {
    RequestHeader request =
        fakeRequestBuilder()
            .method("GET")
            .uri("/programs/1")
            .cookie(Http.Cookie.builder(TIMEOUT_COOKIE_NAME, "somevalue").withPath("/").build())
            .build();
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(mockProfile));
    when(settingsManifest.getSessionTimeoutEnabled(request)).thenReturn(false);

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(200);
    Optional<Http.Cookie> cookie = result.cookies().get(TIMEOUT_COOKIE_NAME);
    assertThat(cookie).isPresent();
    assertThat(cookie.get().maxAge().longValue()).isEqualTo(Duration.ZERO.toSeconds());
  }

  @Test
  public void testTimeoutDisabled_noCookie_passesThrough() throws Exception {
    RequestHeader request = fakeRequestBuilder().method("GET").uri("/programs/1").build();
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(mockProfile));
    when(settingsManifest.getSessionTimeoutEnabled(request)).thenReturn(false);

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(200);
    assertThat(result.cookies().get(TIMEOUT_COOKIE_NAME)).isEmpty();
  }

  // --- Timeout enabled ---

  @Test
  public void testTimeoutEnabled_validSession_setsCookieAndUpdatesActivity() throws Exception {
    RequestHeader request = fakeRequestBuilder().method("GET").uri("/programs/1").build();
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(mockProfile));
    when(settingsManifest.getSessionTimeoutEnabled(request)).thenReturn(true);
    when(sessionTimeoutService.isSessionTimedOut(eq(mockProfile), anyLong())).thenReturn(false);

    Result result = executeFilter(request);

    verify(sessionTimeoutService).calculateTimeoutData(eq(mockProfile), anyLong());
    verify(mockProfileData).updateLastActivityTime(clock);

    Optional<Http.Cookie> cookie = result.cookies().get(TIMEOUT_COOKIE_NAME);
    assertThat(cookie).isPresent();
    assertThat(cookie.get().httpOnly()).isFalse();
    assertThat(cookie.get().path()).isEqualTo("/");
    assertThat(cookie.get().maxAge().longValue()).isEqualTo(Duration.ofDays(2).toSeconds());

    JsonNode timeoutData = decodeTimeoutCookie(cookie.get());
    assertThat(timeoutData.get("currentTime").asLong()).isEqualTo(CURRENT_TIME);
    assertThat(timeoutData.get("inactivityTimeout").asLong())
        .isEqualTo(defaultTimeoutData.inactivityTimeout());
    assertThat(timeoutData.get("inactivityWarning").asLong())
        .isEqualTo(defaultTimeoutData.inactivityWarning());
    assertThat(timeoutData.get("totalTimeout").asLong())
        .isEqualTo(defaultTimeoutData.totalTimeout());
    assertThat(timeoutData.get("totalWarning").asLong())
        .isEqualTo(defaultTimeoutData.totalWarning());
  }

  @Test
  public void testSessionTimedOut_redirectsToLogout() throws Exception {
    RequestHeader request = fakeRequestBuilder().method("GET").uri("/programs/1").build();
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(mockProfile));
    when(settingsManifest.getSessionTimeoutEnabled(request)).thenReturn(true);
    when(sessionTimeoutService.isSessionTimedOut(eq(mockProfile), anyLong())).thenReturn(true);

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(303);
    assertThat(result.redirectLocation()).hasValue("/logout");
    verify(mockAccount).removeActiveSession(SESSION_ID);
    verify(mockAccount).save();
  }

  private Result executeFilter(RequestHeader request) throws Exception {
    EssentialAction action =
        filter.apply(
            EssentialAction.of(requestHeader -> Accumulator.done(play.mvc.Results.ok("Success"))));
    return action.apply(request).run(mat).toCompletableFuture().get();
  }

  private JsonNode decodeTimeoutCookie(Http.Cookie cookie) throws Exception {
    String decodedValue =
        new String(Base64.getDecoder().decode(cookie.value()), StandardCharsets.UTF_8);
    return instanceOf(ObjectMapper.class).readTree(decodedValue);
  }
}
