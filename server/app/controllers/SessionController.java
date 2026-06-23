package controllers;

import auth.ProfileUtils;
import java.time.Clock;
import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

/** Controller for managing user's session. */
public class SessionController extends Controller {
  private final ProfileUtils profileUtils;
  private final Clock clock;

  @Inject
  public SessionController(ProfileUtils profileUtils, Clock clock) {
    this.profileUtils = profileUtils;
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
    return profileUtils
        .optionalCurrentUserProfile(request)
        .map(
            profile -> {
              profile.getProfileData().updateLastSessionActivityTime(clock);
              return ok();
            })
        .orElse(unauthorized());
  }
}
