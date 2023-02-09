package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.img;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;

import auth.AuthIdentityProviderName;
import auth.FakeAdminClient;
import auth.GuestClient;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.routes;
import featureflags.FeatureFlags;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import views.style.BaseStyles;

/** Renders a page for login. */
public class LoginForm extends BaseHtmlView {

  private final BaseHtmlLayout layout;
  private final String civiformImageTag;
  private final boolean renderCreateAccountButton;
  private final AuthIdentityProviderName applicantIdp;
  private final Optional<String> maybeLogoUrl;
  private final String civicEntityFullName;
  private final String civicEntityShortName;
  private final FakeAdminClient fakeAdminClient;
  private final FeatureFlags featureFlags;

  @Inject
  public LoginForm(
      BaseHtmlLayout layout,
      Config config,
      FakeAdminClient fakeAdminClient,
      FeatureFlags featureFlags) {
    this.layout = checkNotNull(layout);
    checkNotNull(config);

    this.civiformImageTag = config.getString("civiform_image_tag");
    this.applicantIdp = AuthIdentityProviderName.fromConfig(config);
    this.maybeLogoUrl =
        config.hasPath("whitelabel.small_logo_url")
            ? Optional.of(config.getString("whitelabel.small_logo_url"))
            : Optional.empty();
    this.civicEntityFullName = config.getString("whitelabel.civic_entity_full_name");
    this.civicEntityShortName = config.getString("whitelabel.civic_entity_short_name");
    this.fakeAdminClient = checkNotNull(fakeAdminClient);
    this.featureFlags = checkNotNull(featureFlags);
    this.renderCreateAccountButton = config.hasPath("auth.register_uri");
  }

  public Content render(Http.Request request, Messages messages, Optional<String> message) {
    String title = "Login";

    HtmlBundle htmlBundle = this.layout.getBundle().setTitle(title);
    htmlBundle.addMainContent(mainContent(request, messages));

    // "defense in depth", sort of - this client won't be present in production, and this button
    // won't show up except when running in an acceptable environment.
    if (fakeAdminClient.canEnable(request.host())) {
      htmlBundle.addMainContent(debugContent());
    }

    return layout.render(htmlBundle);
  }

  private DivTag mainContent(Http.Request request, Messages messages) {
    DivTag content = div().withClasses(BaseStyles.LOGIN_PAGE);

    if (maybeLogoUrl.isPresent()) {
      content.with(
          img()
              .withSrc(maybeLogoUrl.get())
              .withAlt(civicEntityFullName + "Logo")
              .attr("aria-hidden", "true")
              .withClasses("w-1/4", "pt-4"));
    } else {
      content.with(
          this.layout
              .viewUtils
              .makeLocalImageTag("ChiefSeattle_Blue")
              .withAlt(civicEntityFullName + " Logo")
              .attr("aria-hidden", "true")
              .withClasses("w-1/4", "pt-4"));
    }

    content.with(
        h1().withClasses("flex", "text-4xl", "gap-1", "px-8")
            .with(span(civicEntityShortName).withClasses("font-bold"))
            .with(span("CiviForm")));

    DivTag applicantAccountLogin =
        div()
            .withClasses(
                "flex",
                "flex-col",
                "gap-2",
                "py-6",
                "px-8",
                "text-lg",
                "w-full",
                "place-items-center");

    if (applicantIdp == AuthIdentityProviderName.DISABLED_APPLICANT) {
      String loginDisabledMessage =
          messages.at(MessageKey.CONTENT_LOGIN_DISABLED_PROMPT.getKeyName());
      content.with(applicantAccountLogin.with(p(loginDisabledMessage)));
    } else {
      String loginMessage =
          messages.at(MessageKey.CONTENT_LOGIN_PROMPT.getKeyName(), civicEntityFullName);
      content.with(applicantAccountLogin.with(p(loginMessage)).with(loginButton(messages)));
      String alternativeMessage =
          messages.at(MessageKey.CONTENT_LOGIN_PROMPT_ALTERNATIVE.getKeyName());
      content.with(p(alternativeMessage).withClasses("text-lg"));
    }

    DivTag alternativeLoginButtons = div();
    if (renderCreateAccountButton) {
      String or = messages.at(MessageKey.CONTENT_OR.getKeyName());
      alternativeLoginButtons.with(createAccountButton(messages)).with(p(or));
    }
    alternativeLoginButtons
        .with(guestButton(messages))
        .withClasses("pb-12", "px-8", "flex", "gap-4", "items-center", "text-lg");
    content.with(alternativeLoginButtons);

    String adminPrompt = messages.at(MessageKey.CONTENT_ADMIN_LOGIN_PROMPT.getKeyName());
    DivTag footer =
        div()
            .withClasses(
                "bg-gray-100",
                "py-4",
                "px-8",
                "w-full",
                "flex",
                "flex-col",
                "gap-2",
                "justify-center",
                "items-center",
                "text-base")
            .with(p(adminPrompt).with(text(" ")).with(adminLink(messages)));
    if (featureFlags.showCiviformImageTagOnLandingPage(request)) {
      footer.with(p("CiviForm version: " + civiformImageTag).withClasses("text-gray-600"));
    }
    content.with(footer);

    return div().withClasses("fixed", "w-screen", "h-screen", "bg-gray-200").with(content);
  }

  private DivTag debugContent() {
    return div()
        .withClasses("absolute")
        .with(
            p("DEMO MODE. LOGIN AS:").withClasses("text-2xl"),
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

  private ButtonTag loginButton(Messages messages) {
    String msg = messages.at(MessageKey.BUTTON_LOGIN.getKeyName());
    return redirectButton(
            applicantIdp.getValue(),
            msg,
            routes.LoginController.applicantLogin(Optional.empty()).url())
        .withClasses(BaseStyles.LOGIN_REDIRECT_BUTTON);
  }

  private ButtonTag createAccountButton(Messages messages) {
    String msg = messages.at(MessageKey.BUTTON_CREATE_ACCOUNT.getKeyName());
    return redirectButton("register", msg, routes.LoginController.register().url())
        .withClasses(BaseStyles.LOGIN_REDIRECT_BUTTON_SECONDARY);
  }

  private ButtonTag guestButton(Messages messages) {
    String msg = messages.at(MessageKey.BUTTON_LOGIN_GUEST.getKeyName());
    return redirectButton(
            "guest", msg, routes.CallbackController.callback(GuestClient.CLIENT_NAME).url())
        .withClasses(BaseStyles.LOGIN_REDIRECT_BUTTON_SECONDARY);
  }

  private ATag adminLink(Messages messages) {
    String msg = messages.at(MessageKey.LINK_ADMIN_LOGIN.getKeyName());
    return a(msg)
        .withHref(routes.LoginController.adminLogin().url())
        .withClasses(BaseStyles.ADMIN_LOGIN);
  }
}
