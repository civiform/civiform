package views.errors;

import static j2html.TagCreator.h1;
import static j2html.TagCreator.div;
import static j2html.TagCreator.span;
import static j2html.TagCreator.p;
import static j2html.TagCreator.a;
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
import views.style.ApplicantStyles;
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
      String img_author_url = "https://unsplash.com/@lazycreekimages";
      String img_url = "https://unsplash.com/photos/0W4XLGITrHg";
      return div(
                div(
                  h1(messages.at(MessageKey.ERROR_NOT_FOUND_TITLE.getKeyName()))
                    .withClasses(ApplicantStyles.H1_PROGRAM_APPLICATION, Styles.TEXT_CENTER),
                  p(messages.at(MessageKey.ERROR_NOT_FOUND_DESCRIPTION_A.getKeyName())),
                  span(
                      span(messages.at(MessageKey.ERROR_NOT_FOUND_DESCRIPTION_B.getKeyName())),
                      span(" "),
                      a(messages.at(MessageKey.ERROR_NOT_FOUND_DESCRIPTION_C.getKeyName()))
                        .withHref("/"),
                      span(".")
                    ),
                  div(
                    layout.viewUtils.makeLocalImageTag("404")
                      .withClasses(Styles.M_AUTO),
                    span(
                        span(messages.at(MessageKey.ERROR_NOT_FOUND_IMG_CAPTION_A.getKeyName())),
                        span(" "),
                        a(messages.at(MessageKey.ERROR_NOT_FOUND_IMG_CAPTION_B.getKeyName()))
                          .withHref(img_author_url),
                        span(" "),
                        span(messages.at(MessageKey.ERROR_NOT_FOUND_IMG_CAPTION_C.getKeyName())),
                        span(" "),
                        a(messages.at(MessageKey.ERROR_NOT_FOUND_IMG_CAPTION_D.getKeyName()))
                          .withHref(img_url)
                      )
                    )
                  )
                ).withClasses(Styles.TEXT_CENTER, Styles.MAX_W_SCREEN_SM, Styles.W_FULL, Styles.MX_AUTO);
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
