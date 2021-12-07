package views.errors;

import static j2html.TagCreator.h1;
import static j2html.TagCreator.div;
import static j2html.TagCreator.p;
import static j2html.TagCreator.text;

import com.google.inject.Inject;
import play.twirl.api.Content;
import views.style.BaseStyles;
import views.style.Styles;
import views.style.StyleUtils;
import services.MessageKey;
import play.i18n.Messages;
import j2html.tags.ContainerTag;
import play.mvc.Http;
import views.BaseHtmlView;
import views.BaseHtmlLayout;
import views.HtmlBundle;
import views.style.ErrorStyles;
import views.applicant.ApplicantLayout;
import play.i18n.MessagesApi;
import services.MessageKey;

/* Get rid of this pg doesn't seem to exist
   Use a text link
*/

public class NotFound extends BaseHtmlView {

  //private final BaseHtmlLayout layout;
  private final ApplicantLayout layout;
  private final MessagesApi messagesApi;

  @Inject
  public NotFound(ApplicantLayout layout, MessagesApi messagesApi) {
    this.layout = layout;
    this.messagesApi = messagesApi;
  }

  private ContainerTag mainContent(Messages messages) {
      return div(
                div(
                  h1(messages.at(MessageKey.ERROR_NOT_FOUND_TITLE.getKeyName())),
                  p(messages.at(MessageKey.ERROR_NOT_FOUND_DESCRIPTION.getKeyName())),
                  layout.viewUtils.makeLocalImageTag("404")
                )
              ).withClasses(Styles.MAX_W_SCREEN_SM, Styles.W_FULL, Styles.MX_AUTO);
  }

  public Content render(
        Http.Request request,
        Messages messages,
        String applicantName
      ) {
    HtmlBundle bundle = layout.getBundle();
    bundle.addMainContent(mainContent(messages));
    return layout.renderWithNav(request, applicantName, messages, bundle);
  }

  public Content render(
        Http.Request request,
        Messages messages
      ) {
    HtmlBundle bundle = layout.getBundle();
    bundle.addMainContent(mainContent(messages));
    return layout.renderWithNav(request, messages, bundle);
  }
}
