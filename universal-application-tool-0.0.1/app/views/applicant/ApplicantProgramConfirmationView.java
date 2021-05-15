package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;

import com.google.inject.Inject;
import controllers.applicant.routes;
import j2html.tags.ContainerTag;
import java.util.Optional;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.Styles;

public final class ApplicantProgramConfirmationView extends BaseHtmlView {

  private final ApplicantLayout layout;

  @Inject
  public ApplicantProgramConfirmationView(ApplicantLayout layout) {
    this.layout = checkNotNull(layout);
  }

  /** Renders a confirmation page for the applicant's submission. */
  public Content render(
      Http.Request request,
      Long applicantId,
      Long applicationId,
      String programTitle,
      Messages messages,
      Optional<String> banner) {
    HtmlBundle bundle =
        layout.getBundle().setTitle(String.format("Application confirmation — %s", programTitle));

    ContainerTag content = div().withClasses(Styles.MX_16);
    content.with(h2(messages.at("content.confirmed", programTitle, applicationId)));
    content.with(
        new LinkElement()
            .setHref(routes.ApplicantProgramsController.index(applicantId).url())
            .setText(messages.at("content.returnToDash"))
            .asAnchorText());

    if (banner.isPresent()) {
      bundle.addToastMessages(ToastMessage.error(banner.get()));
    }

    bundle.addMainContent(
        layout.renderHeader(100),
        h1(String.format("%s application confirmation", programTitle))
            .withClasses(Styles.PX_16, Styles.PY_4),
        content);

    return layout.renderWithNav(request, messages, bundle);
  }
}
