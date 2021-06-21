package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.p;
import static j2html.TagCreator.text;

import auth.FakeAdminClient;
import auth.GuestClient;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import play.mvc.Http;
import play.twirl.api.Content;
import views.components.ToastMessage;
import views.style.BaseStyles;
import views.style.Styles;

public class LoginForm extends BaseHtmlView {

  private final BaseHtmlLayout layout;
  private final Config config;

  @Inject
  public LoginForm(BaseHtmlLayout layout, Config config) {
    this.layout = checkNotNull(layout);
    this.config = config;
  }

  public Content render(Http.Request request, Optional<String> message) {
    String title = "Login";

    HtmlBundle htmlBundle = this.layout.getBundle().setTitle(title);
    htmlBundle.addMainContent(mainContent());

    // "defense in depth", sort of - this client won't be present in production, and this button
    // won't show up except when running in an acceptable environment.
    if (FakeAdminClient.canEnable(request.host())) {
      htmlBundle.addMainContent(debugContent());
    }

    if (message.isPresent()) {
      String errorString = "Error: You are not logged in. " + message.get();
      htmlBundle.addToastMessages(ToastMessage.error(errorString));
    }

    return layout.render(htmlBundle);
  }

  private ContainerTag mainContent() {
    ContainerTag content = div().withClasses(BaseStyles.LOGIN_PAGE);

    content.with(
        this.layout
            .viewUtils
            .makeLocalImageTag("ChiefSeattle_Blue")
            .withClasses(Styles.W_1_4, Styles.PT_4));
    content.with(
        div()
            .withClasses(Styles.FLEX, Styles.TEXT_4XL, Styles.GAP_1, Styles._MT_6, Styles.PX_8)
            .with(p("Seattle").withClasses(Styles.FONT_BOLD))
            .with(p("CiviForm")));

    String loginMessage = "Please log in with your City of Seattle account";
    content.with(
        div()
            .withClasses(
                Styles.FLEX,
                Styles.FLEX_COL,
                Styles.GAP_2,
                Styles.PY_6,
                Styles.PX_8,
                Styles.TEXT_LG,
                Styles.W_FULL,
                Styles.PLACE_ITEMS_CENTER)
            .with(p(loginMessage))
            .with(loginButton()));

    String alternativeMessage = "Don't have an account?";
    String or = "or";
    content.with(p(alternativeMessage).withClasses(Styles.TEXT_LG));
    ContainerTag alternativeLoginButtons =
        div()
            .withClasses(
                Styles.PB_12,
                Styles.PX_8,
                Styles.FLEX,
                Styles.GAP_4,
                Styles.ITEMS_CENTER,
                Styles.TEXT_LG);
    if (config.hasPath("idcs.register_uri")) {
      alternativeLoginButtons.with(createAccountButton()).with(p(or)).with(guestButton());
    } else {
      alternativeLoginButtons.with(guestButton());
    }
    content.with(alternativeLoginButtons);

    String somethingElse = "Looking for something else?";
    content.with(
        div()
            .withClasses(
                Styles.BG_GRAY_100,
                Styles.PY_4,
                Styles.PX_8,
                Styles.W_FULL,
                Styles.FLEX,
                Styles.GAP_2,
                Styles.JUSTIFY_CENTER,
                Styles.ITEMS_CENTER,
                Styles.TEXT_BASE)
            .with(p(somethingElse).with(text(" ")).with(adminButton())));

    return div()
        .withClasses(Styles.FIXED, Styles.W_SCREEN, Styles.H_SCREEN, Styles.BG_GRAY_200)
        .with(content);
  }

  private ContainerTag debugContent() {
    return div()
        .withClasses(Styles.ABSOLUTE)
        .with(
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

  private Tag loginButton() {
    String msg = "LOG IN";
    return redirectButton(
            "idcs", msg, routes.LoginController.idcsLoginWithRedirect(Optional.empty()).url())
        .withClasses(BaseStyles.LOGIN_REDIRECT_BUTTON);
  }

  private Tag createAccountButton() {
    String msg = "CREATE ACCOUNT";
    return redirectButton("register", msg, routes.LoginController.register().url())
        .withClasses(BaseStyles.LOGIN_REDIRECT_BUTTON_SECONDARY);
  }

  private Tag guestButton() {
    String msg = "CONTINUE AS GUEST";
    return redirectButton(
            "guest", msg, routes.CallbackController.callback(GuestClient.CLIENT_NAME).url())
        .withClasses(BaseStyles.LOGIN_REDIRECT_BUTTON_SECONDARY);
  }

  private Tag adminButton() {
    String msg = "Admin login";
    return a(msg)
        .withHref(routes.LoginController.adfsLogin().url())
        .withClasses(BaseStyles.ADMIN_LOGIN);
  }
}
