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

public class UnsupportedBrowserView extends BaseHtmlView {

  private final ApplicantLayout applicantLayout;

  @Inject
  public UnsupportedBrowserView(ApplicantLayout applicantLayout) {
    this.applicantLayout = checkNotNull(applicantLayout);
  }

  public Content render() {
    HtmlBundle bundle = applicantLayout.getBundle();

    bundle.setTitle("Unsupported browser");
    bundle.addMainContent(h1("Unsupported browser"));
    bundle.addMainContent(
      div(
        "You are using unsupported browser. Please use one the latest browsers Edge, Firefox, Safari or Chrome."));

    return applicantLayout.render(bundle);
  }
}
