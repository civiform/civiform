package views;

import static j2html.TagCreator.body;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.text;

import auth.UatProfile;
import com.google.common.base.Preconditions;
import java.util.Optional;
import javax.inject.Inject;
import play.twirl.api.Content;

public class ProfileView extends BaseHtmlView {

  private BaseHtmlLayout layout;

  @Inject
  public ProfileView(BaseHtmlLayout layout) {
    this.layout = Preconditions.checkNotNull(layout);
  }

  public Content render(Optional<UatProfile> maybeProfile) {
    if (maybeProfile.isPresent()) {
      UatProfile profile = maybeProfile.get();
      return layout.htmlContent(
          body(
              h1(profile.getClientName()),
              h1(profile.getId()).withId("guest-id"),
              h1("Roles"),
              text(profile.getRoles().toString()),
              h1("JSON"),
              text(profile.getApplicant().join().getApplicantData().asJsonString())));
    } else {
      return layout.htmlContent(body(h1("no profile detected")));
    }
  }
}
