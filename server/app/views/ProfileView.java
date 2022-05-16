package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;

import auth.CiviFormProfile;
import j2html.tags.specialized.SpanTag;
import javax.inject.Inject;
import models.Applicant;
import play.twirl.api.Content;

/** Renders a page for viewing user profile. */
public class ProfileView extends BaseHtmlView {

  private final BaseHtmlLayout layout;

  @Inject
  public ProfileView(BaseHtmlLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(CiviFormProfile profile, Applicant applicant) {
    SpanTag applicantIdTag =
        span(String.valueOf(applicant.id))
            .withId("applicant-id")
            .withData("applicant-id", String.valueOf(applicant.id));

    return layout
        .getBundle()
        .setTitle("Profile View - CiviForm")
        .addMainContent(
            div(h1(profile.getClientName())),
            div(h1(String.format("Profile ID: %s", profile.getId())).withId("profile-id")),
            div(h1(text("Applicant ID: "), applicantIdTag)),
            div(h1("Profile Roles")),
            div(span(profile.getRoles().toString())),
            div(h1("Applicant Data JSON")),
            div(span(applicant.getApplicantData().asJsonString())),
            div(h1("Applicant Email Address (if present)")),
            div(span(applicant.getAccount().getEmailAddress())))
        .render();
  }

  public Content renderNoProfile() {
    return layout
        .getBundle()
        .setTitle("Not logged in - CiviForm")
        .addMainContent(div(h1("no profile detected")))
        .render();
  }
}
