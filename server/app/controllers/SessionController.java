package controllers;

import auth.ProfileUtils;
import java.time.Clock;
import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.settings.SettingsManifest;

/** Controller for managing user's session. */
public class SessionController extends Controller {
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
      return badRequest();
    }

    return profileUtils
        .optionalCurrentUserProfile(request)
        .map(
            profile -> {
              profile.getProfileData().updateLastActivityTime(clock);
              return ok();
            })
        .orElse(unauthorized());
  }
}
