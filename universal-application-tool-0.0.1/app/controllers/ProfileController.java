package controllers;

import javax.inject.Inject;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.play.PlayWebContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.ProfileView;

public class ProfileController extends Controller {
  private SessionStore sessionStore;
  private ProfileView profileView;

  @Inject
  public ProfileController(SessionStore sessionStore, ProfileView profileView) {
    this.sessionStore = sessionStore;
    this.profileView = profileView;
  }

  public Result profilePage(Http.Request request) {
    PlayWebContext webContext = new PlayWebContext(request);
    ProfileManager profileManager = new ProfileManager(webContext, sessionStore);
    return ok(profileView.render(profileManager.getProfile()));
  }
}
