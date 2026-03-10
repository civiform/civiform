package filters;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.AccountNonexistentException;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Provider;
import models.AccountModel;
import models.SessionDetails;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.util.ByteString;
import play.libs.Json;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import repository.DatabaseExecutionContext;
import services.session.SessionTimeoutService;
import services.settings.SettingsManifest;

/**
 * A filter to validate user accounts and manage session timeouts.
 *
 * <p>This filter ensures the account referenced in the browser cookie is valid, checks for session
 * expiration, and sets a cookie for the frontend to show timeout warnings.
 */
public class CiviFormSessionFilter extends EssentialFilter {
  private static final String TIMEOUT_COOKIE_NAME = "session_timeout_data";
  private static final Duration COOKIE_MAX_AGE = Duration.ofDays(2);

  private final ProfileUtils profileUtils;
  private final Materializer materializer;
  private final Clock clock;
  private final Provider<SettingsManifest> settingsManifest;
  private final Provider<SessionTimeoutService> sessionTimeoutService;
  private final Provider<DatabaseExecutionContext> databaseExecutionContext;

  @Inject
  public CiviFormSessionFilter(
      ProfileUtils profileUtils,
      Materializer materializer,
      Clock clock,
      Provider<SettingsManifest> settingsManifest,
      Provider<SessionTimeoutService> sessionTimeoutService,
      Provider<DatabaseExecutionContext> databaseExecutionContext) {
    this.profileUtils = checkNotNull(profileUtils);
    this.materializer = checkNotNull(materializer);
    this.clock = checkNotNull(clock);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.sessionTimeoutService = checkNotNull(sessionTimeoutService);
    this.databaseExecutionContext = checkNotNull(databaseExecutionContext);
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request -> {
          if (allowedEndpoint(request)) {
            // Update activity time for allowed endpoints so that requests
            // to these routes (e.g., API calls) count toward session activity
            request
                .attrs()
                .getOptional(ProfileUtils.CURRENT_USER_PROFILE)
                .ifPresent(profile -> profile.getProfileData().updateLastActivityTime(clock));
            return next.apply(request);
          }

          /*
           * Get the profile if it doesn't already exist on the request Should have already been
           * added in {@link CiviFormProfileFilter}
           */
          Optional<CiviFormProfile> optionalProfile =
              request
                  .attrs()
                  .getOptional(ProfileUtils.CURRENT_USER_PROFILE)
                  .or(() -> profileUtils.optionalCurrentUserProfile(request));

          if (optionalProfile.isEmpty()) {
            return next.apply(request)
                .map(this::clearTimeoutCookie, materializer.executionContext());
          }

          CiviFormProfile profile = optionalProfile.get();

          CompletionStage<Accumulator<ByteString, Result>> futureAccumulator =
              profile
                  .getAccount()
                  .thenApplyAsync(Optional::of, databaseExecutionContext.get())
                  .exceptionally(
                      ex -> {
                        Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
                        if (cause instanceof AccountNonexistentException) {
                          return Optional.empty();
                        }
                        throw new CompletionException(cause);
                      })
                  .thenApplyAsync(
                      optionalAccount -> {
                        // Validate account
                        if (optionalAccount.isEmpty()) {
                          return redirectToLogout();
                        }

                        // Validate session
                        AccountModel account = optionalAccount.get();
                        Optional<SessionDetails> optionalSession =
                            account.getActiveSession(profile.getProfileData().getSessionId());

                        if (settingsManifest.get().getSessionReplayProtectionEnabled()
                            && optionalSession.isEmpty()) {
                          return redirectToLogout();
                        }

                        // Validate session length
                        if (settingsManifest.get().getSessionTimeoutEnabled(request)
                            && optionalSession.isPresent()) {
                          long sessionStartTimeInMillis =
                              optionalSession.get().getCreationTime().toEpochMilli();

                          if (sessionTimeoutService
                              .get()
                              .isSessionTimedOut(profile, sessionStartTimeInMillis)) {
                            account.removeActiveSession(profile.getProfileData().getSessionId());
                            account.save();
                            return redirectToLogout();
                          }

                          // Update last activity time
                          profile.getProfileData().updateLastActivityTime(clock);

                          return next.apply(request)
                              .map(
                                  result ->
                                      result.withCookies(
                                          createSessionTimestampCookie(
                                              profile, sessionStartTimeInMillis)),
                                  materializer.executionContext());
                        }

                        return next.apply(request)
                            .map(
                                result -> {
                                  if (request.cookies().get(TIMEOUT_COOKIE_NAME).isPresent()) {
                                    return clearTimeoutCookie(result);
                                  }
                                  return result;
                                },
                                materializer.executionContext());
                      },
                      databaseExecutionContext.get());

          return Accumulator.flatten(futureAccumulator, materializer);
        });
  }

  private static Accumulator<ByteString, Result> redirectToLogout() {
    return Accumulator.done(Results.redirect(org.pac4j.play.routes.LogoutController.logout()));
  }

  /**
   * Return true if the endpoint does not require a profile. Logout url is necessary here to avoid
   * infinite redirect.
   *
   * <p>NOTE: You might think we'd also want an OptionalProfileRoutes check here. However, this is
   * only called after checking if a profile is present. If the profile isn't present, we skip
   * validation anyway. If the profile is present but invalid, we don't want to allow hitting those
   * endpoints with an invalid profile, so we don't add that check here.
   */
  private static boolean allowedEndpoint(Http.RequestHeader requestHeader) {
    return NonUserRoutes.anyMatch(requestHeader) || isLogoutRequest(requestHeader.uri());
  }

  /** Return true if the request is to the logout endpoint. */
  private static boolean isLogoutRequest(String uri) {
    return uri.startsWith(org.pac4j.play.routes.LogoutController.logout().url());
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
   * @param sessionStartTimeInMillis the session start time in milliseconds
   * @return The cookie containing the session's timeout data.
   */
  private Http.Cookie createSessionTimestampCookie(
      CiviFormProfile profile, long sessionStartTimeInMillis) {
    SessionTimeoutService.TimeoutData timeoutData =
        sessionTimeoutService.get().calculateTimeoutData(profile, sessionStartTimeInMillis);

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
  }

  private Result clearTimeoutCookie(Result result) {
    return result.withCookies(
        Http.Cookie.builder(TIMEOUT_COOKIE_NAME, "")
            .withMaxAge(Duration.ZERO)
            .withPath("/")
            .build());
  }
}
