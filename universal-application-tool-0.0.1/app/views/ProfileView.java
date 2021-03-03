package views;

import static j2html.TagCreator.body;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;

import auth.UatProfile;
import com.google.common.base.Preconditions;
import j2html.tags.Tag;
import javax.inject.Inject;
import models.Applicant;
import play.twirl.api.Content;

public class ProfileView extends BaseHtmlView {

  private final BaseHtmlLayout layout;

  @Inject
  public ProfileView(BaseHtmlLayout layout) {
    this.layout = Preconditions.checkNotNull(layout);
  }

  public Content render(UatProfile profile, Applicant applicant) {
    Tag applicantIdTag =
        span(String.valueOf(applicant.id))
            .withId("applicant-id")
            .withData("applicant-id", String.valueOf(applicant.id));

    return layout.htmlContent(
        body(
            h1(profile.getClientName()),
            h1(String.format("Profile ID: %s", profile.getId())).withId("profile-id"),
            h1(text("Applicant ID: "), applicantIdTag),
            h1("Profile Roles"),
            text(profile.getRoles().toString()),
            h1("Applicant Data JSON"),
            text(applicant.getApplicantData().asJsonString())));
  }

  public Content renderNoProfile() {
    return layout.htmlContent(body(h1("no profile detected")));
  }
}
