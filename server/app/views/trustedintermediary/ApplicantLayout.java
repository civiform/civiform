package views.trustedintermediary;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.b;
import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.iff;
import static j2html.TagCreator.img;
import static j2html.TagCreator.input;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;
import static services.applicant.ApplicantPersonalInfo.ApplicantType.GUEST;

import actions.RouteExtractor;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import controllers.routes;
import io.jsonwebtoken.lang.Strings;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.H1Tag;
import j2html.tags.specialized.ImgTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.NavTag;
import j2html.tags.specialized.SelectTag;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import modules.ThymeleafModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.routing.Router;
import play.twirl.api.Content;
import services.BundledAssetsFinder;
import services.DeploymentType;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.settings.SettingsManifest;
import views.BaseHtmlLayout;
import views.HtmlBundle;
import views.LanguageSelector;
import views.ViewUtils;
import views.components.ButtonStyles;
import views.components.Icons;
import views.components.LinkElement;
import views.components.LinkElement.IconPosition;
import views.components.Modal;
import views.components.Modal.Width;
import views.components.PageNotProductionBanner;
import views.dev.DebugContent;
import views.html.helper.CSRF;
import views.style.ApplicantStyles;
import views.style.BaseStyles;
import views.style.StyleUtils;

/** Contains methods rendering common components used across applicant pages. */
public class ApplicantLayout extends BaseHtmlLayout {

  private static final Logger logger = LoggerFactory.getLogger(ApplicantLayout.class);

  private static final Modal DEBUG_CONTENT_MODAL =
      Modal.builder()
          .setModalId("debug-content-modal")
          .setLocation(Modal.Location.DEBUG)
          .setContent(DebugContent.devTools())
          .setModalTitle("Dev tools")
          .setWidth(Width.THIRD)
          .build();

  private final BaseHtmlLayout layout;
  private final ProfileUtils profileUtils;
  private final LanguageUtils languageUtils;
  private final LanguageSelector languageSelector;
  private final boolean isDevOrStaging;
  private final PageNotProductionBanner pageNotProductionBanner;
  private String tiDashboardHref = getTiDashboardHref();
  private final MessagesApi messagesApi;
  private final ApplicantRoutes applicantRoutes;

  @Inject
  public ApplicantLayout(
      BaseHtmlLayout layout,
      ViewUtils viewUtils,
      ProfileUtils profileUtils,
      LanguageSelector languageSelector,
      LanguageUtils languageUtils,
      SettingsManifest settingsManifest,
      DeploymentType deploymentType,
      BundledAssetsFinder bundledAssetsFinder,
      PageNotProductionBanner pageNotProductionBanner,
      MessagesApi messagesApi,
      ApplicantRoutes applicantRoutes,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      TemplateEngine templateEngine) {
    super(
        viewUtils,
        settingsManifest,
        deploymentType,
        bundledAssetsFinder,
        playThymeleafContextFactory,
        templateEngine);
    this.layout = layout;
    this.profileUtils = checkNotNull(profileUtils);
    this.languageSelector = checkNotNull(languageSelector);
    this.languageUtils = checkNotNull(languageUtils);
    this.isDevOrStaging = deploymentType.isDevOrStaging();
    this.pageNotProductionBanner = checkNotNull(pageNotProductionBanner);
    this.messagesApi = checkNotNull(messagesApi);
    this.applicantRoutes = checkNotNull(applicantRoutes);
  }

  @Override
  public Content render(HtmlBundle bundle) {
    // Add the session timeout modals to the bundle if the profile is present
    Optional<CiviFormProfile> profile =
        profileUtils.optionalCurrentUserProfile(bundle.getRequest());
    if (profile.isPresent()) {
      addSessionTimeoutModals(bundle, messagesApi.preferred(bundle.getRequest()));
    }

    bundle.addBodyStyles(ApplicantStyles.BODY);

    bundle.addFooterStyles("mt-24");

    Content rendered = super.render(bundle);
    if (!rendered.body().contains("<h1")) {
      logger.error("Page does not contain an <h1>, which is important for screen readers.");
    }
    if (Strings.countOccurrencesOf(rendered.body(), "<h1") > 1) {
      logger.error("Page contains more than one <h1>, which is detrimental to screen readers.");
    }
    return rendered;
  }

  // Same as renderWithNav, but defaults to no admin login link.
  public Content renderWithNav(
      Http.Request request,
      ApplicantPersonalInfo personalInfo,
      Messages messages,
      HtmlBundle bundle,
      Optional<Long> applicantId) {
    return renderWithNav(
        request,
        personalInfo,
        messages,
        bundle,
        /* includeAdminLogin= */ false,
        /* applicantId= */ applicantId);
  }

  public Content renderWithNav(
      Http.Request request,
      ApplicantPersonalInfo personalInfo,
      Messages messages,
      HtmlBundle bundle,
      boolean includeAdminLogin,
      Optional<Long> applicantId) {
    bundle.addPageNotProductionBanner(pageNotProductionBanner.render(request, messages));

    if (isDevOrStaging && !settingsManifest.getStagingDisableDemoModeLogins(request)) {
      bundle.addModals(DEBUG_CONTENT_MODAL);
    }

    String supportEmail = settingsManifest.getSupportEmailAddress(request).get();
    String language = languageUtils.getPreferredLanguage(request).code();
    bundle.setLanguage(language);
    bundle.addHeaderContent(renderNavBar(request, personalInfo, messages, applicantId));

    ATag emailAction =
        new LinkElement()
            .setText(supportEmail)
            .setHref("mailto:" + supportEmail)
            .opensInNewTab()
            .asAnchorText()
            .withClasses(ApplicantStyles.LINK);
    ATag adminLoginAction =
        new LinkElement()
            .setText(messages.at(MessageKey.LINK_ADMIN_LOGIN.getKeyName()))
            .setHref(routes.LoginController.adminLogin().url())
            .asAnchorText()
            .withClasses(ApplicantStyles.LINK);

    DivTag footerDiv =
        div()
            .withClasses("flex", "flex-col")
            .with(
                div()
                    .with(
                        span(
                                text(
                                    messages.at(
                                        MessageKey.FOOTER_SUPPORT_LINK_DESCRIPTION.getKeyName())),
                                text(" "))
                            .withClasses("text-gray-600"),
                        emailAction)
                    .withClasses("mx-auto"),
                div()
                    .condWith(
                        includeAdminLogin,
                        span(
                                text(
                                    messages.at(
                                        MessageKey.CONTENT_ADMIN_FOOTER_PROMPT.getKeyName())),
                                text(" "),
                                adminLoginAction)
                            .withClasses("text-gray-600"))
                    .withClasses("mx-auto"));

    bundle.addFooterContent(footerDiv);

    return render(bundle);
  }

  private NavTag renderNavBar(
      Http.Request request,
      ApplicantPersonalInfo applicantPersonalInfo,
      Messages messages,
      Optional<Long> applicantId) {
    Optional<CiviFormProfile> profile = profileUtils.optionalCurrentUserProfile(request);

    return nav()
        .condWith(
            !settingsManifest.getShowNotProductionBannerEnabled(request),
            getGovBanner(Optional.of(messages)))
        .with(
            div()
                .withClasses(
                    "bg-white", "border-b", "align-middle", "p-1", "flex", "flex-row", "flex-wrap")
                .with(
                    div(branding(request))
                        .withClasses(
                            "items-center",
                            "place-items-center",
                            "flex-shrink-0",
                            "grow",
                            StyleUtils.responsiveMedium("grow-0")))
                .with(maybeRenderTiButton(profile, messages))
                .condWith(
                    isDevOrStaging && !settingsManifest.getStagingDisableDemoModeLogins(request),
                    div()
                        .withClasses("place-content-center")
                        .with(
                            a("DevTools")
                                .withId(DEBUG_CONTENT_MODAL.getTriggerButtonId())
                                .withClasses(
                                    StyleUtils.joinStyles(ApplicantStyles.LINK, "cursor-pointer"))))
                .with(
                    div(
                            getLanguageForm(request, messages, profile, applicantId),
                            authDisplaySection(applicantPersonalInfo, profile, messages))
                        .withClasses(
                            "flex",
                            "flex-row",
                            "grow",
                            "shrink-0",
                            "place-content-center",
                            "max-w-full",
                            StyleUtils.responsiveMedium("grow-0", "shrink"))))
        .condWith(
            !onTiDashboardPage(request),
            maybeRenderTiBanner(profile, applicantPersonalInfo.getDisplayString(messages)));
  }

  private ContainerTag<?> getLanguageForm(
      Http.Request request,
      Messages messages,
      Optional<CiviFormProfile> profile,
      Optional<Long> applicantId) {
    ContainerTag<?> languageFormDiv = div().withClasses("flex", "flex-col", "justify-center");

    String updateLanguageAction =
        applicantId.isPresent()
            ? controllers.applicant.routes.ApplicantInformationController.setLangFromSwitcher(
                    applicantId.get())
                .url()
            : controllers.applicant.routes.ApplicantInformationController
                .setLangFromSwitcherWithoutApplicant()
                .url();

    String csrfToken = CSRF.getToken(request.asScala()).value();
    InputTag csrfInput = input().isHidden().withValue(csrfToken).withName("csrfToken");
    InputTag redirectInput =
        input()
            .isHidden()
            .withValue(getUpdateLanguageRedirectUri(request, profile, applicantId))
            .withName("redirectLink");
    String preferredLanguage = languageUtils.getPreferredLanguage(request).code();
    SelectTag languageDropdown =
        languageSelector
            .renderDropdown(preferredLanguage)
            .attr("aria-label", messages.at(MessageKey.LANGUAGE_LABEL_SR.getKeyName()));
    languageFormDiv =
        languageFormDiv.with(
            form()
                .withAction(updateLanguageAction)
                .withMethod(Http.HttpVerbs.POST)
                .with(csrfInput)
                .with(redirectInput)
                .with(languageDropdown)
                .with(TagCreator.button().withId("cf-update-lang").withType("submit").isHidden()));
    return languageFormDiv;
  }

  /**
   * Calculate the redirect location after the language is changed. If the current request is a
   * POST, the redirect is be mapped to the associated GET uri.
   */
  private String getUpdateLanguageRedirectUri(
      Request request, Optional<CiviFormProfile> profile, Optional<Long> applicantId) {
    // Default to the current request if it is not a POST or a redirect can't be constructed.
    if (!request.method().equals("POST")
        || !request.attrs().containsKey(Router.Attrs.HANDLER_DEF)) {
      return request.uri();
    }
    RouteExtractor routeExtractor = new RouteExtractor(request);
    if (!routeExtractor.containsKey("programId")) {
      return request.uri();
    }

    long programId = routeExtractor.getParamLongValue("programId");
    // If the language was changed during /submit, redirect to /review
    if (request.path().contains("submit")) {
      String submitRedirectUri =
          applicantId.isPresent() && profile.isPresent()
              ? applicantRoutes.review(profile.get(), applicantId.get(), programId).url()
              : applicantRoutes.review(programId).url();
      return submitRedirectUri;
    }
    // If the language was changed during a block update, redirect to /block/edit or /block/review
    if (routeExtractor.containsKey("blockId") && profile.isPresent() && applicantId.isPresent()) {
      boolean inReview =
          routeExtractor.containsKey("inReview")
              && Boolean.valueOf(routeExtractor.getParamStringValue("inReview"));
      return applicantRoutes
          .blockEditOrBlockReview(
              profile.get(),
              applicantId.get(),
              programId,
              routeExtractor.getParamStringValue("blockId"),
              inReview)
          .url();
    }
    return request.uri();
  }

  private ATag branding(Http.Request request) {
    ImgTag cityImage =
        settingsManifest
            .getCivicEntitySmallLogoUrl()
            .map(url -> img().withSrc(url))
            .orElseGet(() -> this.layout.viewUtils.makeLocalImageTag("civiform-staging"));

    cityImage
        .withAlt(settingsManifest.getWhitelabelCivicEntityFullName(request).get() + " Logo")
        .withClasses("w-16", "py-1");

    return a().withHref(routes.HomeController.index().url())
        .withClasses(
            "flex", "flex-row", "justify-center", StyleUtils.responsiveMedium("justify-left"))
        .with(
            cityImage,
            div()
                .withId("brand-id")
                .withLang(Locale.ENGLISH.toLanguageTag())
                .withClasses(ApplicantStyles.CIVIFORM_LOGO)
                .with(
                    p(
                        iff(
                            !settingsManifest.getHideCivicEntityNameInHeader(request),
                            b(settingsManifest.getWhitelabelCivicEntityShortName(request).get())),
                        span(text(" CiviForm")))));
  }

  private DivTag maybeRenderTiButton(Optional<CiviFormProfile> profile, Messages messages) {
    DivTag div =
        div()
            .withClasses("flex", "flex-col", "justify-center", "items-center", "grow-0", "md:grow");
    if (profile.isPresent() && profile.get().isTrustedIntermediary()) {
      String tiDashboardText = messages.at(MessageKey.BUTTON_VIEW_AND_ADD_CLIENTS.getKeyName());
      div.with(
          a(tiDashboardText)
              .withId("ti-dashboard-link")
              .withHref(tiDashboardHref)
              .withClasses(
                  "w-1/2",
                  "opacity-75",
                  StyleUtils.hover("opacity-100"),
                  ButtonStyles.SOLID_BLUE_TEXT_XL));
    }
    return div;
  }

  private DivTag maybeRenderTiBanner(
      Optional<CiviFormProfile> profile, String applicantDisplayString) {
    DivTag div = div();
    if (profile.isPresent() && profile.get().isTrustedIntermediary()) {
      div.withClasses("flex", "bg-blue-100", "space-x-1.5", "items-center", "px-8", "py-4")
          .withId("ti-banner")
          .with(
              Icons.svg(Icons.INFO).withClasses("w-5"),
              p(
                  "You are applying for "
                      + applicantDisplayString
                      + ".  Are you trying to apply for a different client?"),
              renderTiDashboardLink());
    }
    return div;
  }

  private String getTiDashboardHref() {
    return controllers.ti.routes.TrustedIntermediaryController.dashboard(
            /* nameQuery= */ Optional.empty(),
            /* dayQuery= */ Optional.empty(),
            /* monthQuery= */ Optional.empty(),
            /* yearQuery= */ Optional.empty(),
            /* page= */ Optional.of(1))
        .url();
  }

  private ATag renderTiDashboardLink() {
    LinkElement link =
        new LinkElement()
            .setHref(tiDashboardHref)
            .setText("Select a new client")
            .setId("ti-clients-link")
            .setStyles(ApplicantStyles.LINK);

    return link.asAnchorText();
  }

  /**
   * Shows authentication status and a button to take actions.
   *
   * <p>If the user is a guest, we show a "Log in" and a "Create an account" button. If they are
   * logged in, we show a "Logout" button. If there is no guest user created yet (landed on the home
   * page but hasn't tried applying to anything yet), we don't show the "End session" link.
   */
  private DivTag authDisplaySection(
      ApplicantPersonalInfo personalInfo, Optional<CiviFormProfile> profile, Messages messages) {
    DivTag outsideDiv = div().withClasses("flex", "flex-col", "justify-center", "pr-4");

    boolean isTi = profile.map(CiviFormProfile::isTrustedIntermediary).orElse(false);
    boolean isGuest = personalInfo.getType() == GUEST && !isTi;

    if (isGuest) {
      String loggedInAsMessage = messages.at(MessageKey.GUEST_INDICATOR.getKeyName());
      String endSessionMessage = messages.at(MessageKey.END_SESSION.getKeyName());
      // Ending a guest session is equivalent to "logging out" the guest.
      String endSessionLink = org.pac4j.play.routes.LogoutController.logout().url();
      String logInMessage = messages.at(MessageKey.BUTTON_LOGIN.getKeyName());
      String logInLink = routes.LoginController.applicantLogin(Optional.empty()).url();
      String createAnAccountMessage = messages.at(MessageKey.BUTTON_CREATE_ACCOUNT.getKeyName());
      String createAnAccountLink = routes.LoginController.register().url();

      DivTag loggedInAsDiv =
          div(
              span(loggedInAsMessage).withClasses("text-sm"),
              text(" "),
              a(endSessionMessage)
                  .withHref(endSessionLink)
                  .withClasses(ApplicantStyles.LINK)
                  .withId("logout-button"),
              br());

      return outsideDiv.with(
          div()
              .condWith(profile.isPresent(), loggedInAsDiv)
              .with(
                  a(logInMessage).withHref(logInLink).withClasses(ApplicantStyles.LINK),
                  text("  |  "),
                  a(createAnAccountMessage)
                      .withHref(createAnAccountLink)
                      .withClasses(ApplicantStyles.LINK)));
    }

    // For TIs we use the account email rather than first and last name because
    // TIs usually do not have the latter data available, but will always have
    // an email address because they are authenticated.
    String accountIdentifier =
        isTi ? tiEmailForDisplay(profile.get()) : personalInfo.getDisplayString(messages);

    String loggedInAsMessage = messages.at(MessageKey.USER_NAME.getKeyName(), accountIdentifier);
    String logoutLink = org.pac4j.play.routes.LogoutController.logout().url();
    return outsideDiv.with(
        div(loggedInAsMessage).withClasses("text-sm"),
        a(messages.at(MessageKey.BUTTON_LOGOUT.getKeyName()))
            .withId("logout-button")
            .withHref(logoutLink)
            .withClasses(ApplicantStyles.LINK));
  }

  private String tiEmailForDisplay(CiviFormProfile profile) {
    // CommonProfile.getEmail() can return null, so we guard that with a generic
    // display string.
    String email =
        Optional.ofNullable(profile.getProfileData().getEmail()).orElse("Trusted Intermediary");

    // To ensure a consistent string with browser snapshots, we override the
    // display email.
    if (email.startsWith("fake-trusted-intermediary") && email.endsWith("@example.com")) {
      return "trusted-intermediary@example.com";
    }

    return email;
  }

  protected String renderPageTitleWithBlockProgress(
      String pageTitle, int blockIndex, int totalBlockCount, Messages messages) {
    // While applicant is filling out the application, include the block they are on as part of
    // their progress.
    blockIndex++;
    String blockNumberText =
        messages.at(MessageKey.CONTENT_BLOCK_PROGRESS.getKeyName(), blockIndex, totalBlockCount);
    return String.format("%s — %s", pageTitle, blockNumberText);
  }

  /**
   * The progress indicator is a bit different while an application is being filled out vs for the
   * summary view.
   *
   * <p>While in progress, the current incomplete block is counted towards progress, but will not
   * show full progress while filling out the last block of the program.
   *
   * <p>For the summary view, there is no "current" block, and full progress can be shown.
   */
  protected DivTag renderProgramApplicationTitleAndProgressIndicator(
      String programTitle,
      int blockIndex,
      int totalBlockCount,
      boolean forSummary,
      Messages messages) {

    int progressStepCount = forSummary ? blockIndex : blockIndex + 1;
    int totalStepCount = forSummary ? totalBlockCount : totalBlockCount + 1;
    ArrayList<String> progressClassesList =
        new ArrayList<String>(
            Arrays.asList(
                "transition-all",
                "duration-300",
                "h-full",
                "left-0",
                "top-0",
                "w-auto",
                "flex-grow"));

    // List of divs representing number of progress steps. Used to render correct progress
    // percentage on progress bar.
    ArrayList<DivTag> innerProgressBars = new ArrayList<DivTag>();
    // Render a set of divs with blue civiform background
    for (int i = 0; i < progressStepCount; i++) {
      ArrayList<String> innerProgressBarClasses = new ArrayList<>(progressClassesList);
      innerProgressBarClasses.add(BaseStyles.BG_CIVIFORM_BLUE);
      if (i == 0) {
        innerProgressBarClasses.add("rounded-l-full");
      }
      if (i == progressStepCount - 1) {
        innerProgressBarClasses.add("rounded-r-full");
      }

      DivTag progressInner =
          div().withClasses(innerProgressBarClasses.stream().toArray(String[]::new));
      innerProgressBars.add(progressInner);
    }
    // Render second set of divs with no background color set
    for (int i = progressStepCount; i < totalStepCount; i++) {
      DivTag progressInner = div().withClasses(progressClassesList.stream().toArray(String[]::new));
      innerProgressBars.add(progressInner);
    }
    DivTag progressIndicator =
        div()
            .with(innerProgressBars)
            .withId("progress-indicator")
            .withClasses(
                "border",
                BaseStyles.BORDER_CIVIFORM_BLUE,
                "rounded-full",
                "font-semibold",
                "bg-white",
                "relative",
                "flex",
                "justify-stretch",
                "h-4",
                "mt-4");

    // While applicant is filling out the application, include the block they are on as part of
    // their progress.
    if (!forSummary) {
      blockIndex++;
    }

    String blockNumberText =
        messages.at(MessageKey.CONTENT_BLOCK_PROGRESS.getKeyName(), blockIndex, totalBlockCount);

    H1Tag programTitleContainer =
        h1().withClasses("flex")
            .with(span(programTitle).withClasses(ApplicantStyles.PROGRAM_TITLE))
            .condWith(
                !forSummary,
                span().withClasses("flex-grow"),
                span(blockNumberText).withClasses("text-gray-500", "text-base", "text-right"));

    return div().with(programTitleContainer).with(progressIndicator);
  }

  protected Optional<DivTag> maybeRenderBackToAdminViewButton(
      Http.Request request, long programId) {
    Optional<CiviFormProfile> profile = profileUtils.optionalCurrentUserProfile(request);
    if (profile.isPresent() && profile.get().isCiviFormAdmin()) {
      return Optional.of(
          div()
              .withClasses("mb-6")
              .with(
                  new LinkElement()
                      .setHref(
                          controllers.admin.routes.AdminProgramPreviewController.back(programId)
                              .url())
                      .setIcon(Icons.ARROW_LEFT, IconPosition.START)
                      .setText("Back to admin view")
                      .asAnchorText()));
    }
    return Optional.empty();
  }

  /**
   * Returns true if the request object points to a URI that is the Trusted Intermediary Dashboard
   * or any of its related pages. When a TI is impersonating an applicant to apply for them, this
   * method will return false.
   */
  private static boolean onTiDashboardPage(Http.Request request) {
    String currentPath = null;

    String tiDashboardPath =
        controllers.ti.routes.TrustedIntermediaryController.dashboard(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty())
            .url();

    try {
      URI currentPathUri = new URI(request.uri());
      currentPath = currentPathUri.getPath();
    } catch (URISyntaxException e) {
      logger.error("Could not get the path for uri {}", request.uri());
    }

    return currentPath != null && currentPath.contains(tiDashboardPath);
  }
}
