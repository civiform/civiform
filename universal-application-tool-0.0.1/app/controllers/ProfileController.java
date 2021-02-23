package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.ProfileView;

public class ProfileController extends Controller {
  private ProfileView profileView;
  private ProfileUtils profileUtils;

  @Inject
  public ProfileController(ProfileUtils profileUtils, ProfileView profileView) {
    this.profileUtils = checkNotNull(profileUtils);
    this.profileView = checkNotNull(profileView);
  }

  public Result myProfile(Http.Request request) {
    return ok(profileView.render(profileUtils.currentUserProfile(request)));
  }

  public Result profilePage(Http.Request request, Long id) {
    throw new UnsupportedOperationException("Not implemented yet.");
  }
}
