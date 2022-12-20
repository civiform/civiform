package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.p;

import j2html.tags.specialized.DivTag;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import views.ApplicationBaseView;
import views.HtmlBundle;
import views.style.ApplicantStyles;

public final class IneligibleBlockView extends ApplicationBaseView {

  private final ApplicantLayout layout;

  @Inject
  IneligibleBlockView(ApplicantLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(
      Request request, String programTitle, Optional<String> applicantName, Messages messages) {
    // TODO(#3744): Translate these strings.
    DivTag content =
        div()
            .with(
                h1(
                    String.format(
                        "Based on your responses, you are not eligible for the %s", programTitle)))
            .with(p("You are not eligible for this program."))
            .with(
                p(
                    "You can return to the previous page to edit your answers, or apply to another"
                        + " program."));
    String title = "Ineligible for program";
    HtmlBundle bundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainStyles(ApplicantStyles.MAIN_APPLICANT_INFO)
            .addMainContent(h1(title).withClasses("sr-only"), content);

    return layout.renderWithNav(request, applicantName, messages, bundle);
  }
}
