package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.h1;

import auth.FakeAdminClient;
import auth.GuestClient;
import com.google.inject.Inject;
import controllers.routes;
import java.util.Optional;
import play.mvc.Http;
import play.twirl.api.Content;
import views.components.ToastMessage;
import views.style.Styles;

public class LoginForm extends BaseHtmlView {

  private final BaseHtmlLayout layout;

  @Inject
  public LoginForm(BaseHtmlLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(Http.Request request, Optional<String> message) {
    String title = "Login";

    HtmlBundle htmlBundle =
        this.layout
            .getBundle()
            .setTitle(title)
            .addBodyStyles(Styles.P_4)
            .addMainContent(
                h1("Log In"),
                redirectButton(
                    "idcs",
                    "Login with IDCS (user)",
                    routes.LoginController.idcsLoginWithRedirect(Optional.empty()).url()),
                redirectButton(
                    "adfs", "Login with ADFS (admin)", routes.LoginController.adfsLogin().url()),
                h1("Or, continue as guest."),
                redirectButton(
                    "guest",
                    "Continue",
                    routes.CallbackController.callback(GuestClient.CLIENT_NAME).url()));

    if (message.isPresent()) {
      String errorString = "Error: You are not logged in. " + message.get();
      htmlBundle.addToastMessages(ToastMessage.error(errorString));
    }

    // "defense in depth", sort of - this client won't be present in production, and this button
    // won't show up except when running in an acceptable environment.
    if (FakeAdminClient.canEnable(request.host())) {
      htmlBundle.addMainContent(
          h1("DEBUG MODE: BECOME ADMIN"),
          redirectButton(
              "admin",
              "Global",
              routes.CallbackController.fakeAdmin(
                      FakeAdminClient.CLIENT_NAME, FakeAdminClient.GLOBAL_ADMIN)
                  .url()),
          redirectButton(
              "program-admin",
              "Of All Active Programs",
              routes.CallbackController.fakeAdmin(
                      FakeAdminClient.CLIENT_NAME, FakeAdminClient.PROGRAM_ADMIN)
                  .url()));
    }

    return layout.render(htmlBundle);
  }
}
