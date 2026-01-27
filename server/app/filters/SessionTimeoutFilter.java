package filters;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Provider;
import org.apache.pekko.stream.Materializer;
import play.libs.Json;
import play.mvc.Filter;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import services.session.SessionTimeoutService;
import services.settings.SettingsManifest;

/**
 * A filter to manage session timeouts in CiviForm.
 *
 * <p>NOTE: This filter is currently not called from application.conf because of issues we were
 * seeing with #10330.
 *
 * <p>TODO: #9819 Re-enable this filter when we ensure there aren't performance issues.
 *
 * <p>This filter checks for session timeout, updates last activity time, and sets a cookie for the
 * frontend to show timeout warnings.
 *
 * <p>IMPORTANT: This filter must run after {@link ValidAccountFilter} in the filter chain. It reads
 * the user's profile from request attributes set by ValidAccountFilter to avoid redundant session
 * parsing.
 */
public class SessionTimeoutFilter extends Filter {
  private static final String TIMEOUT_COOKIE_NAME = "session_timeout_data";
  private static final Duration COOKIE_MAX_AGE = Duration.ofDays(2);

  private final Provider<SettingsManifest> settingsManifest;
  private final Provider<SessionTimeoutService> sessionTimeoutService;
  private final Clock clock;

  @Inject
  public SessionTimeoutFilter(
      Materializer mat,
      Provider<SettingsManifest> settingsManifest,
      Provider<SessionTimeoutService> sessionTimeoutService,
      Clock clock) {
    super(mat);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.sessionTimeoutService = checkNotNull(sessionTimeoutService);
    this.clock = checkNotNull(clock);
  }

  @Override
  public CompletionStage<Result> apply(
      Function<Http.RequestHeader, CompletionStage<Result>> nextFilter,
      Http.RequestHeader requestHeader) {
    if (SettingsFilter.areSettingRequestAttributesExcluded(requestHeader)) {
      return nextFilter.apply(requestHeader);
    }

    if (!settingsManifest.get().getSessionTimeoutEnabled(requestHeader)) {
      return nextFilter.apply(requestHeader);
    }
    Optional<CiviFormProfile> optionalProfile =
        requestHeader.attrs().getOptional(ValidAccountFilter.PROFILE_ATTRIBUTE_KEY);
    if (optionalProfile.isEmpty()) {
      return nextFilter.apply(requestHeader).thenApply(this::clearTimeoutCookie);
    }

    CiviFormProfile profile = optionalProfile.get();
    return sessionTimeoutService
        .get()
        .isSessionTimedOut(profile)
        .thenCompose(
            isTimedOut -> {
              if (isTimedOut) {
                return CompletableFuture.completedFuture(
                    Results.redirect(org.pac4j.play.routes.LogoutController.logout()));
              }
              profile.getProfileData().updateLastActivityTime(clock);
              return nextFilter
                  .apply(requestHeader)
                  .thenCompose(
                      result ->
                          createSessionTimestampCookie(profile).thenApply(result::withCookies));
            });
  }

  /**
   * Creates a cookie containing the current session's timeout data.
   *
   * <p>This cookie is intended for use by client-side JavaScript. It is not encrypted because the
   * data it contains is not sensitive, and the client needs to be able to read it. To mitigate
   * potential security risks, this cookie should not contain values that are also transmitted in
   * client requests. If it becomes necessary to use this cookie's data on the server, additional
   * validation measures must be implemented.
   *
   * @param profile Profile corresponding to the logged-in user (applicant or TI).
   * @return A future that completes with the cookie containing the session's timeout data.
   */
  private CompletableFuture<Http.Cookie> createSessionTimestampCookie(CiviFormProfile profile) {
    return sessionTimeoutService
        .get()
        .calculateTimeoutData(profile)
        .thenApply(
            timeoutData -> {
              ObjectNode timestamps =
                  Json.newObject()
                      .put("inactivityWarning", timeoutData.inactivityWarning())
                      .put("inactivityTimeout", timeoutData.inactivityTimeout())
                      .put("totalWarning", timeoutData.totalWarning())
                      .put("totalTimeout", timeoutData.totalTimeout())
                      .put("currentTime", timeoutData.currentTime());

              String cookieValue =
                  Base64.getEncoder()
                      .encodeToString(Json.stringify(timestamps).getBytes(StandardCharsets.UTF_8));

              return Http.Cookie.builder(TIMEOUT_COOKIE_NAME, cookieValue)
                  .withHttpOnly(false)
                  .withPath("/")
                  .withMaxAge(COOKIE_MAX_AGE)
                  .build();
            });
  }

  private Result clearTimeoutCookie(Result result) {
    return result.withCookies(
        Http.Cookie.builder(TIMEOUT_COOKIE_NAME, "")
            .withMaxAge(Duration.ZERO)
            .withPath("/")
            .build());
  }
}
