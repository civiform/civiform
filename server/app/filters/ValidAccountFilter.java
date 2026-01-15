package filters;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Provider;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.util.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import repository.DatabaseExecutionContext;
import services.settings.SettingsManifest;

/**
 * A filter to ensure the account referenced in the browser cookie is valid. This should only matter
 * when the account is deleted from the database which almost will never happen in prod database.
 */
public class ValidAccountFilter extends EssentialFilter {

  private static final Logger logger = LoggerFactory.getLogger(ValidAccountFilter.class);

  private final ProfileUtils profileUtils;
  private final Provider<SettingsManifest> settingsManifest;
  private final Materializer materializer;
  private final Provider<DatabaseExecutionContext> databaseExecutionContext;

  @Inject
  public ValidAccountFilter(
      ProfileUtils profileUtils,
      Provider<SettingsManifest> settingsManifest,
      Materializer materializer,
      Provider<DatabaseExecutionContext> databaseExecutionContext) {
    this.profileUtils = checkNotNull(profileUtils);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.materializer = checkNotNull(materializer);
    this.databaseExecutionContext = databaseExecutionContext;
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request -> {
          if (allowedEndpoint(request)) {
            return next.apply(request);
          }

          Optional<CiviFormProfile> profile = profileUtils.optionalCurrentUserProfile(request);

          if (profile.isEmpty()) {
            return next.apply(request);
          }

          CompletionStage<Accumulator<ByteString, Result>> futureAccumulator =
              shouldLogoutUser(profile.get(), request)
                  .thenApply(
                      shouldLogout -> {
                        if (shouldLogout) {
                          return Accumulator.done(
                              Results.redirect(org.pac4j.play.routes.LogoutController.logout()));
                        } else {
                          return next.apply(request);
                        }
                      });

          return Accumulator.flatten(futureAccumulator, materializer);
        });
  }

  private CompletionStage<Boolean> shouldLogoutUser(
      CiviFormProfile profile, Http.RequestHeader request) {
    // TEST ONLY: Check for special header to simulate filter rejection
    // This check happens INSIDE the async flow so DB queries execute first
    Optional<String> triggerHeader = request.header("X-Test-Trigger-Filter");
    if (triggerHeader.isPresent() && !triggerHeader.get().trim().isEmpty()) {
      // Still execute the DB queries, but force rejection after
      return profileUtils
          .validCiviFormProfile(profile)
          .thenComposeAsync(profileValid -> isValidSession(profile), databaseExecutionContext.get())
          .thenApplyAsync(
              isValidProfileAndSession -> {
                logger.warn(
                    "ValidAccountFilter: DB queries completed, now rejecting due to test header");
                return true; // Force logout
              },
              databaseExecutionContext.get());
    }

    return profileUtils
        .validCiviFormProfile(profile)
        .thenComposeAsync(
            profileValid -> {
              if (!profileValid) {
                return CompletableFuture.completedFuture(false);
              }
              return isValidSession(profile);
            },
            databaseExecutionContext.get())
        // Log out if either profile or session was invalid
        .thenApplyAsync(isValidProfileAndSession -> !isValidProfileAndSession);
  }

  private CompletionStage<Boolean> isValidSession(CiviFormProfile profile) {
    if (settingsManifest.get().getSessionReplayProtectionEnabled()) {
      return profile
          .getAccount()
          .thenApply(
              account ->
                  account.getActiveSession(profile.getProfileData().getSessionId()).isPresent());
    }
    return CompletableFuture.completedFuture(true);
  }

  /**
   * Return true if the endpoint does not require a profile. Logout url is necessary here to avoid
   * infinite redirect.
   *
   * <p>NOTE: You might think we'd also want an OptionalProfileRoutes check here. However, this is
   * currently only called after checking if a profile is present and valid. If the profile isn't
   * present, we never make it here anyway. If the profile is invalid, we don't want to allow
   * hitting those endpoints with an invalid profile, so we don't add that check here.
   */
  private boolean allowedEndpoint(Http.RequestHeader requestHeader) {
    return NonUserRoutes.anyMatch(requestHeader) || isLogoutRequest(requestHeader.uri());
  }

  /** Return true if the request is to the logout endpoint. */
  private boolean isLogoutRequest(String uri) {
    return uri.startsWith(org.pac4j.play.routes.LogoutController.logout().url());
  }
}
