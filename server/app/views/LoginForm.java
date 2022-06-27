package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.img;
import static j2html.TagCreator.p;
import static j2html.TagCreator.text;

import auth.AuthIdentityProviderName;
import auth.FakeAdminClient;
import auth.GuestClient;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import views.style.BaseStyles;
import views.style.Styles;

/** Renders a page for login. */
public class LoginForm extends BaseHtmlView {

  private final BaseHtmlLayout layout;
  private final boolean useIdcsApplicantRegistration;
  private final boolean applicantAuthIsDisabled;
  private final String applicantIdp;
  private final Optional<String> maybeLogoUrl;
  private final String civicEntityFullName;
  private final String civicEntityShortName;
  private final FakeAdminClient fakeAdminClient;

  @Inject
  public LoginForm(BaseHtmlLayout layout, Config config, FakeAdminClient fakeAdminClient) {
    this.layout = checkNotNull(layout);
    checkNotNull(config);

    this.applicantIdp = config.getString("auth.applicant_idp");
    this.maybeLogoUrl =
        config.hasPath("whitelabel.small_logo_url")
            ? Optional.of(config.getString("whitelabel.small_logo_url"))
            : Optional.empty();
    this.civicEntityFullName = config.getString("whitelabel.civic_entity_full_name");
    this.civicEntityShortName = config.getString("whitelabel.civic_entity_short_name");
    this.fakeAdminClient = checkNotNull(fakeAdminClient);

    // Adjust UI for applicant-provider specific settings.
    AuthIdentityProviderName applicantIdpName =
        AuthIdentityProviderName.forString(applicantIdp).get();
    switch (applicantIdpName) {
      case DISABLED_APPLICANT:
        this.applicantAuthIsDisabled = true;
        this.useIdcsApplicantRegistration = false;
        break;
      case IDCS_APPLICANT:
        this.applicantAuthIsDisabled = false;
        this.useIdcsApplicantRegistration = config.hasPath("idcs.register_uri");
        break;
      default:
        this.applicantAuthIsDisabled = false;
        this.useIdcsApplicantRegistration = false;
    }
  }

  public Content render(Http.Request request, Messages messages, Optional<String> message) {
    String title = "Login";

    HtmlBundle htmlBundle = this.layout.getBundle().setTitle(title);
    htmlBundle.addMainContent(mainContent(messages));

    // "defense in depth", sort of - this client won't be present in production, and this button
    // won't show up except when running in an acceptable environment.
    if (fakeAdminClient.canEnable(request.host())) {
      htmlBundle.addMainContent(debugContent());
    }

    return layout.render(htmlBundle);
  }

  private ContainerTag mainContent(Messages messages) {
    ContainerTag content = div().withClasses(BaseStyles.LOGIN_PAGE);

    if (maybeLogoUrl.isPresent()) {
      content.with(
          img()
              .withSrc(maybeLogoUrl.get())
              .withAlt(civicEntityFullName + "Logo")
              .attr("aria-hidden", "true")
              .withClasses(Styles.W_1_4, Styles.PT_4));
    } else {
      content.with(
          this.layout
              .viewUtils
              .makeLocalImageTag("ChiefSeattle_Blue")
              .withAlt(civicEntityFullName + " Logo")
              .attr("aria-hidden", "true")
              .withClasses(Styles.W_1_4, Styles.PT_4));
    }

    content.with(
        div()
            .withClasses(Styles.FLEX, Styles.TEXT_4XL, Styles.GAP_1, Styles._MT_6, Styles.PX_8)
            .with(p(civicEntityShortName).withClasses(Styles.FONT_BOLD))
            .with(p("CiviForm")));

    ContainerTag applicantAccountLogin =
        div()
            .withClasses(
                Styles.FLEX,
                Styles.FLEX_COL,
                Styles.GAP_2,
                Styles.PY_6,
                Styles.PX_8,
                Styles.TEXT_LG,
                Styles.W_FULL,
                Styles.PLACE_ITEMS_CENTER);

    if (applicantAuthIsDisabled) {
      String loginDisabledMessage =
          messages.at(MessageKey.CONTENT_LOGIN_DISABLED_PROMPT.getKeyName());
      applicantAccountLogin = applicantAccountLogin.with(p(loginDisabledMessage));
    } else {
      String loginMessage =
          messages.at(MessageKey.CONTENT_LOGIN_PROMPT.getKeyName(), civicEntityFullName);
      applicantAccountLogin =
          applicantAccountLogin.with(p(loginMessage)).with(loginButton(messages));
      String alternativeMessage =
          messages.at(MessageKey.CONTENT_LOGIN_PROMPT_ALTERNATIVE.getKeyName());
      content.with(p(alternativeMessage).withClasses(Styles.TEXT_LG));
    }
    content.with(applicantAccountLogin);

    ContainerTag alternativeLoginButtons =
        div()
            .withClasses(
                Styles.PB_12,
                Styles.PX_8,
                Styles.FLEX,
                Styles.GAP_4,
                Styles.ITEMS_CENTER,
                Styles.TEXT_LG);
    if (useIdcsApplicantRegistration) {
      String or = messages.at(MessageKey.CONTENT_OR.getKeyName());
      alternativeLoginButtons
          .with(createAccountButton(messages))
          .with(p(or))
          .with(guestButton(messages));
    } else {
      alternativeLoginButtons.with(guestButton(messages));
    }
    content.with(alternativeLoginButtons);

    String adminPrompt = messages.at(MessageKey.CONTENT_ADMIN_LOGIN_PROMPT.getKeyName());
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
            .with(p(adminPrompt).with(text(" ")).with(adminLink(messages))));

    return div()
        .withClasses(Styles.FIXED, Styles.W_SCREEN, Styles.H_SCREEN, Styles.BG_GRAY_200)
        .with(content);
  }

  private ContainerTag debugContent() {
    return div()
        .withClasses(Styles.ABSOLUTE)
        .with(
            h1("DEMO MODE. LOGIN AS:"),
            redirectButton(
                "admin",
                "CiviForm Admin",
                routes.CallbackController.fakeAdmin(
                        FakeAdminClient.CLIENT_NAME, FakeAdminClient.GLOBAL_ADMIN)
                    .url()),
            redirectButton(
                "program-admin",
                "Program Admin",
                routes.CallbackController.fakeAdmin(
                        FakeAdminClient.CLIENT_NAME, FakeAdminClient.PROGRAM_ADMIN)
                    .url()),
            redirectButton(
                "dual-admin",
                "Program and Civiform Admin",
                routes.CallbackController.fakeAdmin(
                        FakeAdminClient.CLIENT_NAME, FakeAdminClient.DUAL_ADMIN)
                    .url()),
            redirectButton(
                "trusted-intermediary",
                "Trusted Intermediary",
                routes.CallbackController.fakeAdmin(
                        FakeAdminClient.CLIENT_NAME, FakeAdminClient.TRUSTED_INTERMEDIARY)
                    .url()));
  }

  private Tag loginButton(Messages messages) {
    String msg = messages.at(MessageKey.BUTTON_LOGIN.getKeyName());
    return redirectButton(
            applicantIdp, msg, routes.LoginController.applicantLogin(Optional.empty()).url())
        .withClasses(BaseStyles.LOGIN_REDIRECT_BUTTON);
  }

  private Tag createAccountButton(Messages messages) {
    String msg = messages.at(MessageKey.BUTTON_CREATE_ACCOUNT.getKeyName());
    return redirectButton("register", msg, routes.LoginController.register().url())
        .withClasses(BaseStyles.LOGIN_REDIRECT_BUTTON_SECONDARY);
  }

  private Tag guestButton(Messages messages) {
    String msg = messages.at(MessageKey.BUTTON_LOGIN_GUEST.getKeyName());
    return redirectButton(
            "guest", msg, routes.CallbackController.callback(GuestClient.CLIENT_NAME).url())
        .withClasses(BaseStyles.LOGIN_REDIRECT_BUTTON_SECONDARY);
  }

  private Tag adminLink(Messages messages) {
    String msg = messages.at(MessageKey.LINK_ADMIN_LOGIN.getKeyName());
    return a(msg)
        .withHref(routes.LoginController.adminLogin().url())
        .withClasses(BaseStyles.ADMIN_LOGIN);
  }
}
