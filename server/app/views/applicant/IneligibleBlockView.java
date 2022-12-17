package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;

import j2html.tags.specialized.DivTag;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import views.ApplicationBaseView;
import views.HtmlBundle;
import views.style.ApplicantStyles;

public class IneligibleBlockView extends ApplicationBaseView {

  private final ApplicantLayout layout;

  @Inject
  IneligibleBlockView(ApplicantLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(Request request, Optional<String> applicantName, Messages messages) {
    DivTag content = div(h2("Sorry you are not eligible for this program"));
    String title = "Ineligible for program";
    HtmlBundle bundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainStyles(ApplicantStyles.MAIN_APPLICANT_INFO)
            .addMainContent(h1(title).withClasses("sr-only"), content);

    // We probably don't want the nav bar here (or we need it somewhat different - no dropdown.)
    return layout.renderWithNav(request, applicantName, messages, bundle);
  }
}
