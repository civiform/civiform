package views.support;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;

import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.applicant.ApplicantLayout;
import views.style.BaseStyles;

public class UnconfirmedIdcsEmailBugView extends BaseHtmlView {
  private static final Logger logger = LoggerFactory.getLogger(UnconfirmedIdcsEmailBugView.class);

  private final ApplicantLayout applicantLayout;

  @Inject
  public UnconfirmedIdcsEmailBugView(ApplicantLayout applicantLayout) {
    this.applicantLayout = checkNotNull(applicantLayout);
  }

  public Content render(Http.Request request) {
    logger.warn("Call make to UnconfirmedIdcsEmailBugView, a Seattle specific error page.");

    HtmlBundle bundle = applicantLayout.getBundle(request);

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
