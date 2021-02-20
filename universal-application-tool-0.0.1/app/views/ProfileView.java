package views;

import static j2html.TagCreator.body;
import static j2html.TagCreator.h1;

import java.util.Optional;
import org.pac4j.core.profile.UserProfile;
import play.twirl.api.Content;

public class ProfileView extends BaseHtmlView {
  public Content render(Optional<UserProfile> maybeProfile) {
    if (maybeProfile.isPresent()) {
      UserProfile profile = maybeProfile.get();
      return htmlContent(
          body(h1(profile.getClientName()), h1(profile.getId()), h1(profile.getUsername())));
    } else {
      return htmlContent(body(h1("no profile detected")));
    }
  }
}
