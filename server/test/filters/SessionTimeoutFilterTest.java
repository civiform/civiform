package filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.apache.pekko.stream.Materializer;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import services.settings.SettingsManifest;

public class SessionTimeoutFilterTest extends WithApplication {

  private SessionTimeoutFilter filter;
  private ProfileUtils profileUtils;
  private SettingsManifest settingsManifest;
  private Materializer materializer;
  private CiviFormProfile mockProfile;
  private CiviFormProfileData mockProfileData;
  private static final String TIMEOUT_COOKIE_NAME = "session_timeout_data";

  @Before
  public void setUp() {
    profileUtils = mock(ProfileUtils.class);
    settingsManifest = mock(SettingsManifest.class);
    materializer = instanceOf(Materializer.class);
    mockProfile = mock(CiviFormProfile.class);
    mockProfileData = mock(CiviFormProfileData.class);

    when(mockProfile.getProfileData()).thenReturn(mockProfileData);

    filter =
        new SessionTimeoutFilter(
            materializer, profileUtils, () -> settingsManifest, instanceOf(Clock.class));
  }

  @Test
  public void testNoProfile_ClearsCookie() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.empty());

    Function<Http.RequestHeader, CompletionStage<Result>> nextFilter =
        requestHeader -> CompletableFuture.completedFuture(play.mvc.Results.ok("Success"));

    Result result = filter.apply(nextFilter, request.build()).toCompletableFuture().get();

    assertThat(result.status()).isEqualTo(200);
    Optional<Http.Cookie> cookie = result.cookies().get(TIMEOUT_COOKIE_NAME);
    assertThat(cookie).isPresent();
    assertThat(cookie.get().maxAge().intValue()).isEqualTo(0);
    assertThat(cookie.get().value()).isEmpty();
  }

  @Test
  public void testTimeoutDisabled_ClearsCookie() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(settingsManifest.getSessionTimeoutEnabled()).thenReturn(false);

    Function<Http.RequestHeader, CompletionStage<Result>> nextFilter =
        requestHeader -> CompletableFuture.completedFuture(play.mvc.Results.ok("Success"));

    Result result = filter.apply(nextFilter, request.build()).toCompletableFuture().get();

    assertThat(result.status()).isEqualTo(200);
    Optional<Http.Cookie> cookie = result.cookies().get(TIMEOUT_COOKIE_NAME);
    assertThat(cookie).isPresent();
    assertThat(cookie.get().maxAge().intValue()).isEqualTo(0);
    assertThat(cookie.get().value()).isEmpty();
  }

  @Test
  public void testTimeoutEnabled_SetsCookie() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(settingsManifest.getSessionTimeoutEnabled()).thenReturn(true);

    long currentTime = System.currentTimeMillis();
    long lastActivityTime = currentTime - (5 * 60 * 1000); // 5 minutes ago
    long sessionStartTime = currentTime - (60 * 60 * 1000); // 1 hour ago

    when(mockProfileData.getLastActivityTime(any())).thenReturn(lastActivityTime);
    when(mockProfile.getSessionStartTime()).thenReturn(Optional.of(sessionStartTime));

    when(settingsManifest.getSessionInactivityTimeoutMinutes()).thenReturn(Optional.of(30));
    when(settingsManifest.getMaximumSessionDurationMinutes()).thenReturn(Optional.of(480));
    when(settingsManifest.getSessionInactivityWarningMinutes()).thenReturn(Optional.of(5));
    when(settingsManifest.getSessionMaxDurationWarningMinutes()).thenReturn(Optional.of(10));

    Function<Http.RequestHeader, CompletionStage<Result>> nextFilter =
        requestHeader -> CompletableFuture.completedFuture(play.mvc.Results.ok("Success"));

    Result result = filter.apply(nextFilter, request.build()).toCompletableFuture().get();

    assertThat(result.status()).isEqualTo(200);
    Optional<Http.Cookie> cookie = result.cookies().get(TIMEOUT_COOKIE_NAME);
    assertThat(cookie).isPresent();
    assertThat(cookie.get().maxAge().longValue()).isEqualTo(Duration.ofDays(2).toSeconds());
    assertThat(cookie.get().value()).isNotEmpty();

    String decodedValue =
        new String(Base64.getDecoder().decode(cookie.get().value()), StandardCharsets.UTF_8);
    JsonNode jsonNode = new ObjectMapper().readTree(decodedValue);

    assertThat(jsonNode.has("currentTime")).isTrue();
    assertThat(jsonNode.has("inactivityWarning")).isTrue();
    assertThat(jsonNode.has("inactivityTimeout")).isTrue();
    assertThat(jsonNode.has("totalWarning")).isTrue();
    assertThat(jsonNode.has("totalTimeout")).isTrue();

    long expectedInactivityTimeout =
        (lastActivityTime / 1000) + (30 * 60); // 30 minutes from last activity
    long expectedInactivityWarning =
        expectedInactivityTimeout - (5 * 60); // 5 minutes before timeout
    long expectedTotalTimeout =
        (sessionStartTime / 1000) + (480 * 60); // 8 hours from session start
    long expectedTotalWarning = expectedTotalTimeout - (10 * 60); // 10 minutes before timeout

    assertThat(jsonNode.get("inactivityTimeout").asLong()).isEqualTo(expectedInactivityTimeout);
    assertThat(jsonNode.get("inactivityWarning").asLong()).isEqualTo(expectedInactivityWarning);
    assertThat(jsonNode.get("totalTimeout").asLong()).isEqualTo(expectedTotalTimeout);
    assertThat(jsonNode.get("totalWarning").asLong()).isEqualTo(expectedTotalWarning);
  }

  @Test
  public void testTimeoutEnabled_WithDefaultValues() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(settingsManifest.getSessionTimeoutEnabled()).thenReturn(true);

    long currentTime = System.currentTimeMillis();
    long lastActivityTime = currentTime - (5 * 60 * 1000); // 5 minutes ago
    long sessionStartTime = currentTime - (60 * 60 * 1000); // 1 hour ago

    when(mockProfileData.getLastActivityTime(any())).thenReturn(lastActivityTime);
    when(mockProfile.getSessionStartTime()).thenReturn(Optional.of(sessionStartTime));

    when(settingsManifest.getSessionInactivityTimeoutMinutes()).thenReturn(Optional.empty());
    when(settingsManifest.getMaximumSessionDurationMinutes()).thenReturn(Optional.empty());
    when(settingsManifest.getSessionInactivityWarningMinutes()).thenReturn(Optional.empty());
    when(settingsManifest.getSessionMaxDurationWarningMinutes()).thenReturn(Optional.empty());

    Function<Http.RequestHeader, CompletionStage<Result>> nextFilter =
        requestHeader -> CompletableFuture.completedFuture(play.mvc.Results.ok("Success"));

    Result result = filter.apply(nextFilter, request.build()).toCompletableFuture().get();

    assertThat(result.status()).isEqualTo(200);
    Optional<Http.Cookie> cookie = result.cookies().get(TIMEOUT_COOKIE_NAME);
    assertThat(cookie).isPresent();

    String decodedValue =
        new String(Base64.getDecoder().decode(cookie.get().value()), StandardCharsets.UTF_8);
    JsonNode jsonNode = new ObjectMapper().readTree(decodedValue);

    long expectedInactivityTimeout = (lastActivityTime / 1000) + (30 * 60); // Default 30 minutes
    long expectedInactivityWarning =
        expectedInactivityTimeout - (5 * 60); // Default 5 minutes warning
    long expectedTotalTimeout =
        (sessionStartTime / 1000) + (600 * 60); // Default 600 minutes (10 hours)
    long expectedTotalWarning = expectedTotalTimeout - (10 * 60); // Default 10 minutes warning

    assertThat(jsonNode.get("inactivityTimeout").asLong()).isEqualTo(expectedInactivityTimeout);
    assertThat(jsonNode.get("inactivityWarning").asLong()).isEqualTo(expectedInactivityWarning);
    assertThat(jsonNode.get("totalTimeout").asLong()).isEqualTo(expectedTotalTimeout);
    assertThat(jsonNode.get("totalWarning").asLong()).isEqualTo(expectedTotalWarning);
  }

  @Test
  public void testCookieProperties() throws Exception {
    Http.RequestBuilder request = fakeRequestBuilder().method("GET").uri("/programs/1");
    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(mockProfile));
    when(settingsManifest.getSessionTimeoutEnabled()).thenReturn(true);

    when(mockProfileData.getLastActivityTime(any())).thenReturn(System.currentTimeMillis());
    when(mockProfile.getSessionStartTime()).thenReturn(Optional.of(System.currentTimeMillis()));

    Function<Http.RequestHeader, CompletionStage<Result>> nextFilter =
        requestHeader -> CompletableFuture.completedFuture(play.mvc.Results.ok("Success"));

    Result result = filter.apply(nextFilter, request.build()).toCompletableFuture().get();

    Optional<Http.Cookie> cookie = result.cookies().get(TIMEOUT_COOKIE_NAME);
    assertThat(cookie).isPresent();
    assertThat(cookie.get().httpOnly()).isFalse();
    assertThat(cookie.get().path()).isEqualTo("/");
    assertThat(cookie.get().maxAge().longValue()).isEqualTo(Duration.ofDays(2).toSeconds());
  }
}
