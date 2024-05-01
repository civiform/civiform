package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;

import auth.CiviFormProfile;
import j2html.tags.specialized.SpanTag;
import javax.inject.Inject;
import models.ApplicantModel;
import play.mvc.Http;
import play.twirl.api.Content;

/** Renders a page for viewing user profile. */
public class ProfileView extends BaseHtmlView {

  private final BaseHtmlLayout layout;

  @Inject
  public ProfileView(BaseHtmlLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(Http.Request request, CiviFormProfile profile, ApplicantModel applicant) {
    SpanTag applicantIdTag =
        span(String.valueOf(applicant.id))
            .withId("applicant-id")
            .withData("applicant-id", String.valueOf(applicant.id));

    return layout
        .getBundle(request)
        .setTitle("Profile view - CiviForm")
        .addMainContent(
            h1(profile.getClientName()),
            h1(String.format("Profile ID: %s", profile.getId())).withId("profile-id"),
            h1(text("Applicant ID: "), applicantIdTag),
            h1("Profile roles"),
            span(profile.getRoles().toString()),
            h1("Applicant data JSON"),
            span(applicant.getApplicantData().asJsonString()),
            h1("Applicant email address (if present)"),
            span(applicant.getAccount().getEmailAddress()))
        .render();
  }

  public Content renderNoProfile(Http.Request request) {
    return layout
        .getBundle(request)
        .setTitle("Not logged in - CiviForm")
        .addMainContent(h1("no profile detected"))
        .render();
  }
}
