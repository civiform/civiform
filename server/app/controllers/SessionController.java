package controllers;

import auth.ProfileUtils;
import java.time.Clock;
import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.settings.SettingsManifest;

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

  public Result extendSession(Http.Request request) {
    if (!settingsManifest.getSessionTimeoutEnabled()) {
      return badRequest("Session timeout is not enabled");
    }

    return profileUtils
        .optionalCurrentUserProfile(request)
        .map(
            profile -> {
              profile.getProfileData().updateLastActivityTime(clock);
              return ok("Session extended");
            })
        .orElse(unauthorized("No active session"));
  }
}
