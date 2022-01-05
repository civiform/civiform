package views.support;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;

import javax.inject.Inject;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.applicant.ApplicantLayout;

public class UnconfirmedIdcsEmailBugView extends BaseHtmlView {

  private final ApplicantLayout applicantLayout;

  @Inject
  public UnconfirmedIdcsEmailBugView(ApplicantLayout applicantLayout) {
    this.applicantLayout = checkNotNull(applicantLayout);
  }

  public Content render() {
    HtmlBundle bundle = applicantLayout.getBundle();

    bundle.setTitle("Please contact support");
    bundle.addMainContent(h1("Please contact support"));
    bundle.addMainContent(
        div(
            "There has been an error with your CiviForm account. Please contact support at the"
                + " City of Seattle at 206-555-5555"));

    return applicantLayout.render(bundle);
  }
}
