package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.inject.Inject;
import java.util.Optional;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.export.JsonPrettifier;

/** Controller to display user profile, if present, in browser. */
public final class ProfileController extends Controller {
  private final ProfileUtils profileUtils;

  @Inject
  ProfileController(ProfileUtils profileUtils) {
    this.profileUtils = checkNotNull(profileUtils);
  }

  /** Returns a text/plain page containing the user profile, if present. */
  public Result index(Http.Request request) {
    Optional<CiviFormProfile> maybeProfile = profileUtils.currentUserProfile(request);
    String profileContent =
        maybeProfile
            .map((p) -> JsonPrettifier.asPrettyJsonString(p.getProfileData()))
            .orElse("No profile present");
    return ok(profileContent).as(Http.MimeTypes.TEXT);
  }
}
