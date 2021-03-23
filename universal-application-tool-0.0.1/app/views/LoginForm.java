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
import views.style.StyleUtils;
import views.style.Styles;

public class LoginForm extends BaseHtmlView {
  private final BaseHtmlLayout layout;
  private final String BANNER_TEXT = "DO NOT enter actual or personal data in this demo site";

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

    return layout.htmlContent(head(layout.tailwindStyles()), demoBanner(), bodyTag);
  }

  public ContainerTag demoBanner() {
    return div()
        .withClass(Styles.BG_RED_600)
        .with(
            div()
                .withClasses(
                    Styles.MAX_W_7XL,
                    Styles.MX_AUTO,
                    Styles.PY_3,
                    Styles.PX_3,
                    StyleUtils.responsiveSmall(Styles.PX_6),
                    StyleUtils.responsiveLarge(Styles.PX_8))
                .with(
                    div()
                        .withClasses(
                            Styles.FLEX,
                            Styles.ITEMS_CENTER,
                            Styles.JUSTIFY_BETWEEN,
                            Styles.FLEX_WRAP)
                        .with(
                            div()
                                .withClasses(
                                    Styles.W_0, Styles.FLEX_1, Styles.FLEX, Styles.ITEMS_CENTER)
                                .with(
                                    span()
                                        .withClasses(
                                            Styles.FLEX,
                                            Styles.P_2,
                                            Styles.ROUNDED_LG,
                                            Styles.BG_RED_800)
                                        .with(
                                            new ContainerTag("svg")
                                                .withClasses(
                                                    Styles.H_6, Styles.W_6, Styles.TEXT_WHITE)
                                                .attr("xmlns", "http://www.w3.org/2000/svg")
                                                .attr("fill", "none")
                                                .attr("viewBox", "0 0 24 24")
                                                .attr("stroke", "currentColor")
                                                .attr("aria-hidden", "true")
                                                .with(
                                                    new ContainerTag("path")
                                                        .attr("stroke-linecap", "round")
                                                        .attr("stroke-linejoin", "round")
                                                        .attr("stroke-width", "2")
                                                        .attr("d", Icons.LOGIN_BANNER_PATH))),
                                    p().withClasses(
                                            Styles.ML_3,
                                            Styles.FONT_MEDIUM,
                                            Styles.TEXT_WHITE,
                                            Styles.TRUNCATE)
                                        .with(
                                            span(BANNER_TEXT)
                                                .withClasses(
                                                    StyleUtils.responsiveMedium(Styles.HIDDEN)),
                                            span(BANNER_TEXT)
                                                .withClasses(
                                                    Styles.HIDDEN,
                                                    StyleUtils.responsiveMedium(
                                                        Styles.INLINE)))))));
  }
}
