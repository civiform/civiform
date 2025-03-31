package filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.test.WithApplication;
import services.session.SessionTimeoutService;
import services.settings.SettingsManifest;

public class SessionTimeoutFilterTest extends WithApplication {
  private static final String TIMEOUT_COOKIE_NAME = "session_timeout_data";
  private static final long CURRENT_TIME = 1000000000L;

  private SessionTimeoutFilter filter;
  private ProfileUtils profileUtils;
  private SettingsManifest settingsManifest;
  private SessionTimeoutService sessionTimeoutService;
  private Materializer materializer;
  private CiviFormProfile mockProfile;
  private SessionTimeoutService.TimeoutData defaultTimeoutData =
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
    sessionTimeoutService = mock(SessionTimeoutService.class);
    materializer = instanceOf(Materializer.class);
    mockProfile = mock(CiviFormProfile.class);

    filter =
        new SessionTimeoutFilter(
            materializer, profileUtils, () -> settingsManifest, () -> sessionTimeoutService);

    // Setup default timeout data

    when(sessionTimeoutService.calculateTimeoutData(any()))
        .thenReturn(CompletableFuture.completedFuture(defaultTimeoutData));
  }

  @Test
  public void testNoProfile_clearsCookie() throws Exception {
    RequestHeader request = fakeRequestBuilder().method("GET").uri("/programs/1").build();
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.empty());

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(200);
    Optional<Http.Cookie> cookie = result.cookies().get(TIMEOUT_COOKIE_NAME);
    assertThat(cookie).isPresent();
    assertThat(cookie.get().maxAge().intValue()).isEqualTo(0);
    assertThat(cookie.get().value()).isEmpty();
  }

  @Test
  public void testTimeoutDisabled_clearsCookie() throws Exception {
    RequestHeader request = fakeRequestBuilder().method("GET").uri("/programs/1").build();
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(mockProfile));
    when(settingsManifest.getSessionTimeoutEnabled(request)).thenReturn(false);

    Result result = executeFilter(request);

    assertThat(result.status()).isEqualTo(200);
    Optional<Http.Cookie> cookie = result.cookies().get(TIMEOUT_COOKIE_NAME);
    assertThat(cookie).isPresent();
    assertThat(cookie.get().maxAge().intValue()).isEqualTo(0);
    assertThat(cookie.get().value()).isEmpty();
  }

  @Test
  public void testTimeoutEnabled_setsCookie() throws Exception {
    RequestHeader request = fakeRequestBuilder().method("GET").uri("/programs/1").build();
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(mockProfile));
    when(settingsManifest.getSessionTimeoutEnabled(request)).thenReturn(true);

    Result result = executeFilter(request);

    verify(sessionTimeoutService).calculateTimeoutData(mockProfile);

    Optional<Http.Cookie> cookie = result.cookies().get(TIMEOUT_COOKIE_NAME);
    assertThat(cookie).isPresent();

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
  public void testCookieProperties() throws Exception {
    RequestHeader request = fakeRequestBuilder().method("GET").uri("/programs/1").build();
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(mockProfile));
    when(settingsManifest.getSessionTimeoutEnabled(request)).thenReturn(true);

    Result result = executeFilter(request);

    Optional<Http.Cookie> cookie = result.cookies().get(TIMEOUT_COOKIE_NAME);
    assertThat(cookie).isPresent();
    assertThat(cookie.get().httpOnly()).isFalse();
    assertThat(cookie.get().path()).isEqualTo("/");
    assertThat(cookie.get().maxAge().longValue()).isEqualTo(Duration.ofDays(2).toSeconds());
  }

  private Result executeFilter(RequestHeader request) throws Exception {
    Function<Http.RequestHeader, CompletionStage<Result>> nextFilter =
        requestHeader -> CompletableFuture.completedFuture(play.mvc.Results.ok("Success"));
    return filter.apply(nextFilter, request).toCompletableFuture().get();
  }

  private JsonNode decodeTimeoutCookie(Http.Cookie cookie) throws Exception {
    String decodedValue =
        new String(Base64.getDecoder().decode(cookie.value()), StandardCharsets.UTF_8);
    return new ObjectMapper().readTree(decodedValue);
  }
}
