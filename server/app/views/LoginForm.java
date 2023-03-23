package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static featureflags.FeatureFlag.NEW_LOGIN_FORM_ENABLED;
import static featureflags.FeatureFlag.SHOW_CIVIFORM_IMAGE_TAG_ON_LANDING_PAGE;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.hr;
import static j2html.TagCreator.img;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.strong;
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
import j2html.tags.specialized.ImgTag;
import java.util.Optional;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.DeploymentType;
import services.MessageKey;
import views.components.Icons;
import views.style.BaseStyles;

/** Renders a page for login. */
public class LoginForm extends BaseHtmlView {

  private final BaseHtmlLayout layout;
  private final String civiformImageTag;
  private final String civiformVersion;
  private final boolean isDevOrStaging;
  private final boolean disableDemoModeLogins;
  private final boolean disableApplicantGuestLogin;
  private final boolean renderCreateAccountButton;
  private final AuthIdentityProviderName applicantIdp;
  private final Optional<String> maybeLogoUrl;
  private final String civicEntityFullName;
  private final String civicEntityShortName;
  private final FeatureFlags featureFlags;

  @Inject
  public LoginForm(
      BaseHtmlLayout layout,
      Config config,
      FeatureFlags featureFlags,
      DeploymentType deploymentType) {
    this.layout = checkNotNull(layout);
    checkNotNull(config);
    checkNotNull(deploymentType);

    this.civiformImageTag = config.getString("civiform_image_tag");
    this.civiformVersion = config.getString("civiform_version");
    this.isDevOrStaging = deploymentType.isDevOrStaging();
    this.disableDemoModeLogins =
        this.isDevOrStaging && config.getBoolean("staging_disable_demo_mode_logins");
    this.disableApplicantGuestLogin =
        this.isDevOrStaging && config.getBoolean("staging_disable_applicant_guest_login");
    this.applicantIdp = AuthIdentityProviderName.fromConfig(config);
    this.maybeLogoUrl =
        config.hasPath("whitelabel.small_logo_url")
            ? Optional.of(config.getString("whitelabel.small_logo_url"))
            : Optional.empty();
    this.civicEntityFullName = config.getString("whitelabel.civic_entity_full_name");
    this.civicEntityShortName = config.getString("whitelabel.civic_entity_short_name");
    this.featureFlags = checkNotNull(featureFlags);
    this.renderCreateAccountButton = true; // config.hasPath("auth.register_uri");
  }

  public Content render(Http.Request request, Messages messages, Optional<String> message) {
    // DO NOT MERGE: remove the ! on this flag check.
    if (!featureFlags.getFlagEnabled(request, NEW_LOGIN_FORM_ENABLED)) {
      return renderNewLoginPage(request, messages);
    } else {
      return renderOldLoginPage(request, messages);
    }
  }

  private Content renderNewLoginPage(Http.Request request, Messages messages) {
    String title = messages.at(MessageKey.TITLE_LOGIN.getKeyName());

    HtmlBundle htmlBundle = this.layout.getBundle().setTitle(title);

    DivTag content = div().withClasses(BaseStyles.LOGIN_PAGE);

    content.with(logo());

    content.with(
        h1().withClasses("flex", "text-4xl", "gap-1", "px-8")
            .with(span(civicEntityShortName).withClasses("font-bold"))
            .with(span("CiviForm")));

    String resourceHelpText =
        messages.at(MessageKey.CIVIFORM_EXPLANATION.getKeyName(), civicEntityFullName);
    content.with(p(resourceHelpText).withClasses("text-base", "text-center", "px-8", "w-5/6"));

    content.with(primaryLoginSectionNew(messages));
    content.with(hr().withClasses("w-10/12", "border-gray-300"));
    content.with(alternativeLoginSectionNew(messages));
    content.with(adminLoginSectionNew(messages, request));

    htmlBundle.addMainContent(
        div().withClasses("fixed", "w-screen", "h-screen", "bg-gray-200").with(content));

    if (isDevOrStaging && !disableDemoModeLogins) {
      htmlBundle.addMainContent(debugContent());
    }

    return layout.render(htmlBundle);
  }

  private Content renderOldLoginPage(Http.Request request, Messages messages) {
    String title = messages.at(MessageKey.TITLE_LOGIN.getKeyName());

    HtmlBundle htmlBundle = this.layout.getBundle().setTitle(title);

    DivTag content = div().withClasses(BaseStyles.LOGIN_PAGE);

    content.with(logo());

    content.with(
        h1().withClasses("flex", "text-4xl", "gap-1", "px-8")
            .with(span(civicEntityShortName).withClasses("font-bold"))
            .with(span("CiviForm")));

    content.with(primaryLoginSectionOld(messages));
    content.with(alternativeLoginSectionOld(messages));
    content.with(adminLoginSectionOld(messages, request));

    htmlBundle.addMainContent(
        div().withClasses("fixed", "w-screen", "h-screen", "bg-gray-200").with(content));

    if (isDevOrStaging && !disableDemoModeLogins) {
      htmlBundle.addMainContent(debugContent());
    }

    return layout.render(htmlBundle);
  }

  private ImgTag logo() {
    if (maybeLogoUrl.isPresent()) {
      return img()
          .withSrc(maybeLogoUrl.get())
          .withAlt(civicEntityFullName + " Logo")
          .attr("aria-hidden", "true")
          .withClasses("w-1/4", "pt-4");
    } else {
      return this.layout
          .viewUtils
          .makeLocalImageTag("ChiefSeattle_Blue")
          .withAlt(civicEntityFullName + " Logo")
          .attr("aria-hidden", "true")
          .withClasses("w-1/4", "pt-4");
    }
  }

  private DivTag primaryLoginSectionOld(Messages messages) {
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
      applicantAccountLogin.with(p(loginDisabledMessage));
    } else {
      String loginMessage =
          messages.at(MessageKey.CONTENT_LOGIN_PROMPT.getKeyName(), civicEntityFullName);
      applicantAccountLogin.with(p(loginMessage)).with(loginButtonOld(messages));
    }

    return applicantAccountLogin;
  }

  private DivTag primaryLoginSectionNew(Messages messages) {
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

    // TODO: i18n
    DivTag tooltip =
        ViewUtils.makeSvgToolTip(
            "This is the same account you may use to pay your City of Seattle utility bill. ",
            Icons.INFO,
            Optional.of("right-0"));

    if (applicantIdp == AuthIdentityProviderName.DISABLED_APPLICANT) {
      String loginDisabledMessage =
          messages.at(MessageKey.CONTENT_LOGIN_DISABLED_PROMPT.getKeyName());
      applicantAccountLogin.with(p(loginDisabledMessage));
    } else {
      String loginMessage = messages.at(MessageKey.LOGIN_CTA.getKeyName(), civicEntityFullName);
      applicantAccountLogin
          .with(
              div(p(loginMessage).withClasses("mr-2", "text-center", "text-gray-500"), tooltip)
                  .withClasses("flex", "flex-row"))
          .with(loginButtonNew(messages));
    }

    return applicantAccountLogin;
  }

  private DivTag alternativeLoginSectionOld(Messages messages) {
    DivTag alternativeLoginDiv = div();

    DivTag alternativeLoginButtons = div();
    // Render the "Don't have an account?" text if either button will be rendered.
    if (renderCreateAccountButton || !disableApplicantGuestLogin) {
      String alternativeMessage =
          messages.at(MessageKey.CONTENT_LOGIN_PROMPT_ALTERNATIVE.getKeyName());
      alternativeLoginDiv.with(p(alternativeMessage).withClasses("text-lg", "text-center", "mb-2"));
    }

    if (renderCreateAccountButton) {
      alternativeLoginButtons.with(createAccountButtonOld(messages));
    }
    if (!disableApplicantGuestLogin) {
      // Only include 'or' if the create account button was rendered.
      if (renderCreateAccountButton) {
        String or = messages.at(MessageKey.CONTENT_OR.getKeyName());
        alternativeLoginButtons.with(p(or));
      }
      alternativeLoginButtons
          .with(guestButton(messages))
          .withClasses("pb-12", "px-8", "flex", "gap-4", "items-center", "text-lg");
    }
    alternativeLoginDiv.with(alternativeLoginButtons);

    return alternativeLoginDiv;
  }

  private DivTag alternativeLoginSectionNew(Messages messages) {
    DivTag alternativeLoginDiv = div();

    // Render the "Don't have an account?" text and help text if either button will be rendered.
    if (renderCreateAccountButton || !disableApplicantGuestLogin) {
      String alternativeMessage =
          messages.at(MessageKey.CONTENT_LOGIN_PROMPT_ALTERNATIVE.getKeyName());
      String alternativeHelpText = messages.at(MessageKey.CREATE_ACCOUNT_EXPLANATION.getKeyName());
      alternativeLoginDiv.with(
          p(strong(alternativeMessage)).withClasses("text-lg", "text-center", "mt-2", "mx-auto"));
      alternativeLoginDiv.with(
          p(alternativeHelpText)
              .withClasses(
                  "text-lg", "text-center", "mb-2", "w-10/12", "mx-auto", "text-gray-500"));
    }

    DivTag alternativeLoginButtons = div();
    if (renderCreateAccountButton) {
      alternativeLoginButtons.with(createAccountButtonNew(messages));
    }
    if (!disableApplicantGuestLogin) {
      alternativeLoginButtons
          .with(guestLink(messages))
          .withClasses("pb-12", "px-8", "flex", "flex-col", "gap-4", "items-center", "text-lg");
    }
    alternativeLoginDiv.with(alternativeLoginButtons);

    return alternativeLoginDiv;
  }

  private DivTag adminLoginSectionOld(Messages messages, Http.Request request) {
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
            .with(p(adminPrompt).with(text(" ")).with(adminLinkOld(messages)));

    if (featureFlags.getFlagEnabled(request, SHOW_CIVIFORM_IMAGE_TAG_ON_LANDING_PAGE)) {
      // civiformVersion is the version the deployer requests, like "latest" or
      // "v1.18.0". civiformImageTag is set by bin/build-prod and is a string
      // like "SNAPSHOT-3af8997-1678895722".
      String version = civiformVersion;
      if (civiformVersion.equals("") || civiformVersion.equals("latest")) {
        version = civiformImageTag;
      }
      footer.with(p("CiviForm version: " + version).withClasses("text-gray-600"));
    }

    return footer;
  }

  private DivTag adminLoginSectionNew(Messages messages, Http.Request request) {
    String adminPrompt = messages.at(MessageKey.CONTENT_ADMIN_LOGIN_PROMPT2.getKeyName());
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
            .with(
                p(adminPrompt)
                    .with(text(" "))
                    .withClass("text-gray-500")
                    .with(adminLinkNew(messages)));

    if (featureFlags.getFlagEnabled(request, SHOW_CIVIFORM_IMAGE_TAG_ON_LANDING_PAGE)) {
      // civiformVersion is the version the deployer requests, like "latest" or
      // "v1.18.0". civiformImageTag is set by bin/build-prod and is a string
      // like "SNAPSHOT-3af8997-1678895722".
      String version = civiformVersion;
      if (civiformVersion.equals("") || civiformVersion.equals("latest")) {
        version = civiformImageTag;
      }
      footer.with(p("CiviForm version: " + version).withClasses("text-gray-600"));
    }

    return footer;
  }

  private ButtonTag loginButtonOld(Messages messages) {
    String msg = messages.at(MessageKey.BUTTON_LOGIN.getKeyName());
    return redirectButton(
            applicantIdp.getValue(),
            msg,
            routes.LoginController.applicantLogin(Optional.empty()).url())
        .withClasses(BaseStyles.LOGIN_REDIRECT_BUTTON_OLD);
  }

  private ButtonTag loginButtonNew(Messages messages) {
    String msg = messages.at(MessageKey.BUTTON_LOGIN.getKeyName());
    return redirectButton(
            applicantIdp.getValue(),
            msg,
            routes.LoginController.applicantLogin(Optional.empty()).url())
        .withClasses(BaseStyles.LOGIN_REDIRECT_BUTTON_NEW);
  }

  private ButtonTag createAccountButtonOld(Messages messages) {
    String msg = messages.at(MessageKey.BUTTON_CREATE_ACCOUNT.getKeyName());
    return redirectButton("register", msg, routes.LoginController.register().url())
        .withClasses(BaseStyles.LOGIN_REDIRECT_BUTTON_SECONDARY);
  }

  private ButtonTag createAccountButtonNew(Messages messages) {
    String msg = messages.at(MessageKey.BUTTON_CREATE_ACCOUNT.getKeyName());
    return redirectButton("register", msg, routes.LoginController.register().url())
        .withClasses(BaseStyles.LOGIN_REDIRECT_BUTTON_SECONDARY_NEW);
  }

  private ButtonTag guestButton(Messages messages) {
    String msg = messages.at(MessageKey.BUTTON_LOGIN_GUEST.getKeyName());
    return redirectButton(
            "guest", msg, routes.CallbackController.callback(GuestClient.CLIENT_NAME).url())
        .withClasses(BaseStyles.LOGIN_REDIRECT_BUTTON_SECONDARY);
  }

  private ATag guestLink(Messages messages) {
    String msg = messages.at(MessageKey.BUTTON_LOGIN_GUEST2.getKeyName());
    return a(msg)
        .withHref(routes.CallbackController.callback(GuestClient.CLIENT_NAME).url())
        .withClasses(BaseStyles.GUEST_LOGIN);
  }

  private ATag adminLinkOld(Messages messages) {
    String msg = messages.at(MessageKey.LINK_ADMIN_LOGIN.getKeyName());
    return a(msg)
        .withHref(routes.LoginController.adminLogin().url())
        .withClasses(BaseStyles.ADMIN_LOGIN);
  }

  private ATag adminLinkNew(Messages messages) {
    String msg = messages.at(MessageKey.LINK_ADMIN_LOGIN.getKeyName());
    return a(msg)
        .withHref(routes.LoginController.adminLogin().url())
        .withClasses(BaseStyles.ADMIN_LOGIN_NEW);
  }

  private DivTag debugContent() {
    return div()
        .withClasses("absolute", "flex", "flex-col")
        .with(
            p("DEVELOPMENT MODE TOOLS:").withClasses("text-2xl"),
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
                    .url()),
            redirectButton(
                "feature-flags",
                "Feature Flags",
                controllers.dev.routes.FeatureFlagOverrideController.index().url()),
            redirectButton(
                "database-seed",
                "Seed Database",
                controllers.dev.routes.DatabaseSeedController.index().url()));
  }
}
