package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.head;

import auth.FakeAdminClient;
import auth.GuestClient;
import com.google.inject.Inject;
import controllers.routes;
import j2html.tags.ContainerTag;
import java.util.Optional;
import play.mvc.Http;
import play.twirl.api.Content;
import views.components.ToastMessage;

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
                div(
                    h1("Log In"),
                    redirectButton(
                        "idcs", "Login with IDCS (user)", routes.LoginController.idcsLogin().url()),
                    redirectButton(
                        "adfs",
                        "Login with ADFS (admin)",
                        routes.LoginController.adfsLogin().url()))),
            div(
                h1("Or, continue as guest."),
                redirectButton(
                    "guest",
                    "Continue",
                    routes.CallbackController.callback(GuestClient.CLIENT_NAME).url())));

    if (!message.orElse("").equals("login")) {
      String errorString = "Error: You are not logged in.";
      bodyTag.with(ToastMessage.error(errorString).getContainerTag());
    }

    // "defense in depth", sort of - this client won't be present in production, and this button
    // won't show up except when running in an acceptable environment.
    if (FakeAdminClient.canEnable(request.host())) {
      bodyTag.with(
          div(
              h1("DEBUG MODE: BECOME ADMIN"),
              redirectButton(
                  "admin",
                  "Continue",
                  routes.CallbackController.callback(FakeAdminClient.CLIENT_NAME).url())));
    }

    return layout.htmlContent(head(layout.tailwindStyles()), bodyTag);
  }
}
