package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.head;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import auth.FakeAdminClient;
import auth.GuestClient;
import com.google.inject.Inject;
import controllers.routes;
import j2html.tags.ContainerTag;
import java.util.Optional;
import play.mvc.Http;
import play.twirl.api.Content;
import views.components.Icons;
import views.components.ToastContainer;
import views.components.ToastMessage;
import views.style.StyleUtils;
import views.style.Styles;

public class LoginForm extends BaseHtmlView {

  private final BaseHtmlLayout layout;

  @Inject
  public LoginForm(BaseHtmlLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(Http.Request request, Optional<String> message) {
    if (message.isPresent()) {
        String errorString = "Error: You are not logged in." + message.orElse("");
        ToastMessage toast = ToastMessage.error(errorString);
        ToastContainer.addMessage(toast);
    }
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
