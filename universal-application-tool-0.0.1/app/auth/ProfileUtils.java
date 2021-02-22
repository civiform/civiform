package auth;

import com.google.common.base.Preconditions;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.play.PlayWebContext;
import play.mvc.Http;

public class ProfileUtils {
  private SessionStore sessionStore;

  @Inject
  public ProfileUtils(SessionStore sessionStore) {
    this.sessionStore = Preconditions.checkNotNull(sessionStore);
  }

  public Optional<UatProfile> currentUserProfile(Http.Request request) {
    // Fetch the current profile from the session cookie, which the ProfileManager
    // will fetch from the request's cookies, using the session store to decrypt it.
    PlayWebContext webContext = new PlayWebContext(request);
    ProfileManager profileManager = new ProfileManager(webContext, sessionStore);
    return profileManager.getProfile(UatProfile.class);
  }
}
