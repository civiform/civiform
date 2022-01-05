package views.support;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;

import javax.inject.Inject;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.applicant.ApplicantLayout;
import views.style.BaseStyles;

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
            "Thank you for logging into CiviForm to apply for City of Seattle programs."
                + " Unfortunately there was an issue verifying your account. Please re-login and"
                + " try again. We apologize for the inconvenience. If you have any questions or"
                + " concerns, please email civiform@seattle.gov or call (206) 256-5515."));
    bundle.addMainContent(
        a("Return to login page").withHref("/").withClasses(BaseStyles.ADMIN_LOGIN));

    return applicantLayout.render(bundle);
  }
}
