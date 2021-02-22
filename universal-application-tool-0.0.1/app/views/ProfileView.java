package views;

import static j2html.TagCreator.body;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.text;

import auth.UatProfile;
import java.util.Optional;
import play.twirl.api.Content;

public class ProfileView extends BaseHtmlView {
  public Content render(Optional<UatProfile> maybeProfile) {
    if (maybeProfile.isPresent()) {
      UatProfile profile = maybeProfile.get();
      return htmlContent(
          body(
              h1(profile.getClientName()),
              h1(profile.getId()),
              h1("Roles"),
              text(profile.getRoles().toString()),
              h1("JSON"),
              text(profile.getApplicant().join().getApplicantData().asJsonString())));
    } else {
      return htmlContent(body(h1("no profile detected")));
    }
  }
}
