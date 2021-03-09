package auth;

import com.google.common.base.Preconditions;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.play.PlayWebContext;
import play.mvc.Http;

public class ProfileUtils {
  private SessionStore sessionStore;
  private ProfileFactory profileFactory;

  @Inject
  public ProfileUtils(SessionStore sessionStore, ProfileFactory profileFactory) {
    this.sessionStore = Preconditions.checkNotNull(sessionStore);
    this.profileFactory = Preconditions.checkNotNull(profileFactory);
  }

  public Optional<UatProfile> currentUserProfile(Http.Request request) {
    // Fetch the current profile from the session cookie, which the ProfileManager
    // will fetch from the request's cookies, using the session store to decrypt it.
    PlayWebContext webContext = new PlayWebContext(request);
    return currentUserProfile(webContext);
  }

  public Optional<UatProfile> currentUserProfile(WebContext webContext) {
    ProfileManager profileManager = new ProfileManager(webContext, sessionStore);
    Optional<UatProfileData> p = profileManager.getProfile(UatProfileData.class);
    if (p.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(profileFactory.wrapProfileData(p.get()));
  }
}
