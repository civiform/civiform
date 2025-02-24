package filters;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Provider;
import org.apache.pekko.stream.Materializer;
import play.libs.Json;
import play.mvc.Filter;
import play.mvc.Http;
import play.mvc.Result;
import services.settings.SettingsManifest;

public class SessionTimeoutFilter extends Filter {
  private static final String TIMEOUT_COOKIE_NAME = "session_timeout_data";
  private static final Duration COOKIE_MAX_AGE = Duration.ofDays(2);

  private final ProfileUtils profileUtils;
  private final Provider<SettingsManifest> settingsManifest;
  private final Clock clock;

  @Inject
  public SessionTimeoutFilter(
      Materializer mat,
      ProfileUtils profileUtils,
      Provider<SettingsManifest> settingsManifest,
      Clock clock) {
    super(mat);
    this.profileUtils = checkNotNull(profileUtils);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.clock = checkNotNull(clock);
  }

  @Override
  public CompletionStage<Result> apply(
      Function<Http.RequestHeader, CompletionStage<Result>> nextFilter,
      Http.RequestHeader requestHeader) {

    Optional<CiviFormProfile> profileOpt = profileUtils.optionalCurrentUserProfile(requestHeader);
    if (!settingsManifest.get().getSessionTimeoutEnabled() || profileOpt.isEmpty()) {
      return nextFilter.apply(requestHeader).thenApply(this::clearTimeoutCookie);
    }
    return nextFilter
        .apply(requestHeader)
        .thenApply(result -> result.withCookies(createSessionTimestampCookie(profileOpt.get())));
  }

  private Http.Cookie createSessionTimestampCookie(CiviFormProfile profile) {
    long currentTime = clock.instant().toEpochMilli() / 1000;
    // Get timestamps in seconds
    long sessionStartTimeInMillis = profile.getSessionStartTime().orElse(currentTime);
    long lastActivityTime = profile.getProfileData().getLastActivityTime(clock) / 1000;
    long sessionStartTime = sessionStartTimeInMillis / 1000;

    // Get configuration values
    int inactivityMinutes = settingsManifest.get().getSessionInactivityTimeoutMinutes().orElse(30);
    int totalLengthMinutes = settingsManifest.get().getMaximumSessionDurationMinutes().orElse(600);
    int inactivityWarningMinutes =
        settingsManifest.get().getSessionInactivityWarningMinutes().orElse(5);
    int durationWarningMinutes =
        settingsManifest.get().getSessionMaxDurationWarningMinutes().orElse(10);

    // Calculate limits
    long inactivityLimit =
        Instant.ofEpochSecond(lastActivityTime)
            .plus(inactivityMinutes, ChronoUnit.MINUTES)
            .getEpochSecond();
    long totalLengthLimit =
        Instant.ofEpochSecond(sessionStartTime)
            .plus(totalLengthMinutes, ChronoUnit.MINUTES)
            .getEpochSecond();

    // Calculate warnings
    long inactivityWarning =
        Instant.ofEpochSecond(inactivityLimit)
            .minus(inactivityWarningMinutes, ChronoUnit.MINUTES)
            .getEpochSecond();
    long totalLengthWarning =
        Instant.ofEpochSecond(totalLengthLimit)
            .minus(durationWarningMinutes, ChronoUnit.MINUTES)
            .getEpochSecond();

    // Generate JSON
    ObjectNode timestamps =
        Json.newObject()
            .put("inactivityWarning", inactivityWarning)
            .put("inactivityTimeout", inactivityLimit)
            .put("totalWarning", totalLengthWarning)
            .put("totalTimeout", totalLengthLimit)
            .put("currentTime", currentTime);
    // Encode the JSON string to Base64 to ensure cookie value validity
    String cookieValue =
        Base64.getEncoder()
            .encodeToString(Json.stringify(timestamps).getBytes(StandardCharsets.UTF_8));

    // Set Cookie with appropriate max age
    return Http.Cookie.builder(TIMEOUT_COOKIE_NAME, cookieValue)
        .withHttpOnly(false)
        .withPath("/")
        .withMaxAge(COOKIE_MAX_AGE)
        .build();
  }

  private Result clearTimeoutCookie(Result result) {
    return result.withCookies(
        Http.Cookie.builder(TIMEOUT_COOKIE_NAME, "")
            .withMaxAge(Duration.ZERO)
            .withPath("/")
            .build());
  }
}
