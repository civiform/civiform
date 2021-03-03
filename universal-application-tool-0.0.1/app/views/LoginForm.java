package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;

import auth.FakeAdminClient;
import auth.GuestClient;
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
            div(
                h1("Error: You are not logged in")
                    .withCondHidden(!message.orElse("").equals("login")),
                h1("TODO: IDCS integration")),
            div(
                h1("Or, continue as guest."),
                redirectButton(
                    "guest",
                    "Continue",
                    routes.CallbackController.callback(GuestClient.CLIENT_NAME).url())));

    // "defense in depth", sort of - this client won't be present in production, and this button
    // won't show up except when running locally.
    if (request.host().startsWith("localhost:")) {
      bodyTag.with(
          div(
              h1("DEBUG MODE: BECOME ADMIN"),
              redirectButton(
                  "admin",
                  "Continue",
                  routes.CallbackController.callback(FakeAdminClient.CLIENT_NAME).url())));
    }

    return layout.htmlContent(bodyTag);
  }
}
