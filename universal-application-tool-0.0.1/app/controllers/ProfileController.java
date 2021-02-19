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

  public Result myProfile(Http.Request request) {
    // Fetch the current profile from the session cookie, which the ProfileManager
    // will fetch from the request's cookies, using the session store to decrypt it.
    PlayWebContext webContext = new PlayWebContext(request);
    ProfileManager profileManager = new ProfileManager(webContext, sessionStore);
    return ok(profileView.render(profileManager.getProfile()));
  }

  public Result profilePage(Http.Request request, Long id) {
    throw new UnsupportedOperationException("Not implemented yet.");
  }
}
