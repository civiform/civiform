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
import services.MessageKey;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.ApplicantStyles;

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
      String userName,
      Long applicationId,
      String programTitle,
      Messages messages,
      Optional<String> banner) {
    String pageTitle = "Application confirmation";
    HtmlBundle bundle =
        layout.getBundle().setTitle(String.format("%s — %s", pageTitle, programTitle));

    ContainerTag content =
        div()
            .with(
                h2(
                    messages.at(
                        MessageKey.CONTENT_CONFIRMED.getKeyName(), programTitle, applicationId)))
            .with(
                new LinkElement()
                    .setHref(routes.ApplicantProgramsController.index(applicantId).url())
                    .setText(messages.at(MessageKey.LINK_RETURN_TO_DASH.getKeyName()))
                    .asAnchorText());

    if (banner.isPresent()) {
      bundle.addToastMessages(ToastMessage.error(banner.get()));
    }

    bundle
        .addMainContent(
            layout.renderProgramApplicationTitleAndProgressIndicator(programTitle),
            h1(pageTitle).withClasses(ApplicantStyles.H1_PROGRAM_APPLICATION),
            content)
        .addMainStyles(ApplicantStyles.MAIN_PROGRAM_APPLICATION);

    return layout.renderWithNav(request, userName, messages, bundle);
  }
}
