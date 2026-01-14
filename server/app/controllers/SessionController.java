package controllers;

import auth.ProfileUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.settings.SettingsManifest;

/** Controller for managing user's session. */
public class SessionController extends Controller {
  private static final Logger logger = LoggerFactory.getLogger(SessionController.class);
  private static final ZoneId PST_ZONE = ZoneId.of("America/Los_Angeles");
  private static final DateTimeFormatter PST_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(PST_ZONE);
  private final ProfileUtils profileUtils;
  private final SettingsManifest settingsManifest;
  private final Clock clock;

  @Inject
  public SessionController(
      ProfileUtils profileUtils, SettingsManifest settingsManifest, Clock clock) {
    this.profileUtils = profileUtils;
    this.settingsManifest = settingsManifest;
    this.clock = clock;
  }

  /**
   * Extends user session by updating the last activity time.
   *
   * <p>This endpoint is called by the frontend to extend the session for the current user.
   *
   * @param request the current request
   * @return a 200 response if the session was successfully extended, a 400 response if session
   *     timeouts are disabled
   */
  public Result extendSession(Http.Request request) {
    if (!settingsManifest.getSessionTimeoutEnabled()) {
      logger.warn("Session timeout is disabled - rejecting extend session request");
      return badRequest();
    }

    return profileUtils
        .optionalCurrentUserProfile(request)
        .map(
            profile -> {
              long oldActivityTime = profile.getProfileData().getLastActivityTime(clock);
              profile.getProfileData().updateLastActivityTime(clock);
              long newActivityTime = profile.getProfileData().getLastActivityTime(clock);
              logger.info(
                  "Session extended | Old activity time: {} | New activity time: {} | Current time:"
                      + " {}",
                  PST_FORMATTER.format(Instant.ofEpochMilli(oldActivityTime)),
                  PST_FORMATTER.format(Instant.ofEpochMilli(newActivityTime)),
                  PST_FORMATTER.format(Instant.ofEpochMilli(clock.millis())));
              return ok();
            })
        .orElse(unauthorized());
  }
}
