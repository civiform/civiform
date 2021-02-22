package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.body;
import static j2html.TagCreator.h1;

import com.google.inject.Inject;
import controllers.routes;
import j2html.tags.ContainerTag;
import java.util.Optional;
import play.mvc.Http;
import play.twirl.api.Content;

public class LoginForm extends BaseHtmlView {
  private final BaseHtmlLayout layout;

  @Inject
  public LoginForm(BaseHtmlLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(Http.Request request, Optional<String> message) {
    ContainerTag bodyTag =
        body(
            h1("Error: You are not logged in").withCondHidden(!message.orElse("").equals("login")),
            h1("TODO: IDCS integration"),
            h1("Or, continue as guest."),
            redirectButton(
                "guest", "Continue", routes.CallbackController.callback("GuestClient").url()));
    if (request.host().equals("localhost")) {
      bodyTag =
          bodyTag.with(
              redirectButton(
                  "guest", "Continue", routes.CallbackController.callback("GuestClient").url()));
    }
    return layout.htmlContent(bodyTag);
  }
}
