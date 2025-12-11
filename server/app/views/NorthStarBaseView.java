package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static services.applicant.ApplicantPersonalInfo.ApplicantType.GUEST;

import actions.RouteExtractor;
import auth.CiviFormProfile;
import auth.FakeAdminClient;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import controllers.routes;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Lang;
import play.i18n.Messages;
import play.mvc.Http.Request;
import play.routing.Router;
import services.AlertSettings;
import services.AlertType;
import services.BundledAssetsFinder;
import services.DeploymentType;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.settings.SettingsManifest;
import views.components.Icons;
import views.html.helper.CSRF;

public abstract class NorthStarBaseView {
  protected final TemplateEngine templateEngine;
  protected final ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory;
  private final BundledAssetsFinder bundledAssetsFinder;
  protected final ApplicantRoutes applicantRoutes;
  protected final SettingsManifest settingsManifest;
  protected final LanguageUtils languageUtils;
  protected final boolean isDevOrStaging;
  protected static final String THEME_PRIMARY_HEX = "#005ea2";
  protected static final String THEME_PRIMARY_DARKER_HEX = "#162e51";

  protected NorthStarBaseView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      BundledAssetsFinder bundledAssetsFinder,
      ApplicantRoutes applicantRoutes,
      SettingsManifest settingsManifest,
      LanguageUtils languageUtils,
      DeploymentType deploymentType) {
    this.templateEngine = checkNotNull(templateEngine);
    this.playThymeleafContextFactory = checkNotNull(playThymeleafContextFactory);
    this.bundledAssetsFinder = checkNotNull(bundledAssetsFinder);
    this.applicantRoutes = checkNotNull(applicantRoutes);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.languageUtils = checkNotNull(languageUtils);
    this.isDevOrStaging = checkNotNull(deploymentType).isDevOrStaging();
  }

  protected ThymeleafModule.PlayThymeleafContext createThymeleafContext(
      Request request,
      Optional<Long> applicantId,
      Optional<CiviFormProfile> profile,
      ApplicantPersonalInfo applicantPersonalInfo,
      Messages messages) {
    ThymeleafModule.PlayThymeleafContext context = playThymeleafContextFactory.create(request);
    context.setVariable("civiformImageTag", settingsManifest.getCiviformImageTag().get());
    context.setVariable("addNoIndexMetaTag", settingsManifest.getStagingAddNoindexMetaTag());
    context.setVariable("favicon", settingsManifest.getFaviconUrl().orElse(""));
    context.setVariable("mapQuestionEnabled", settingsManifest.getMapQuestionEnabled(request));

    context.setVariable("useBundlerDevServer", bundledAssetsFinder.useBundlerDevServer());
    context.setVariable("viteClientUrl", bundledAssetsFinder.viteClientUrl());
    context.setVariable("northStarStylesheet", bundledAssetsFinder.getNorthStarStylesheet());
    context.setVariable("mapLibreGLStylesheet", bundledAssetsFinder.getMapLibreGLStylesheet());
    context.setVariable("applicantJsBundle", bundledAssetsFinder.getApplicantJsBundle());
    context.setVariable("uswdsJsInit", bundledAssetsFinder.getUswdsJsInit());
    context.setVariable("uswdsJsBundle", bundledAssetsFinder.getUswdsJsBundle());

    context.setVariable("cspNonce", CspUtil.getNonce(request));
    context.setVariable("csrfToken", CSRF.getToken(request.asScala()).value());
    context.setVariable("optionalMeasurementId", settingsManifest.getMeasurementId());
    context.setVariable(
        "smallLogoUrl",
        settingsManifest
            .getCivicEntitySmallLogoUrl()
            .orElse(bundledAssetsFinder.path("Images/civiform-staging.png")));
    context.setVariable(
        "hideCivicEntityName", settingsManifest.getHideCivicEntityNameInHeader(request));
    context.setVariable(
        "civicEntityShortName", settingsManifest.getWhitelabelCivicEntityShortName(request).get());
    context.setVariable(
        "civicEntityFullName", settingsManifest.getWhitelabelCivicEntityFullName(request).get());
    context.setVariable("adminLoginUrl", routes.LoginController.adminLogin().url());
    context.setVariable("closeIcon", Icons.CLOSE);
    context.setVariable("httpsIcon", bundledAssetsFinder.path("Images/uswds/icon-https.svg"));
    context.setVariable("govIcon", bundledAssetsFinder.path("Images/uswds/icon-dot-gov.svg"));
    context.setVariable(
        "locationIcon", bundledAssetsFinder.path("Images/uswds/icon-location_on.png"));
    context.setVariable(
        "selectedLocationIcon",
        bundledAssetsFinder.path("Images/uswds/icon-location_selected.png"));
    context.setVariable("supportEmail", settingsManifest.getSupportEmailAddress(request).get());
    boolean userIsAdmin = profile.map(CiviFormProfile::isCiviFormAdmin).orElse(false);
    context.setVariable("userIsAdmin", userIsAdmin);
    context.setVariable("goBackIcon", Icons.ARROW_LEFT);
    context.setVariable("launchIcon", Icons.LAUNCH);

    // Language selector params
    Lang preferredLanguage = languageUtils.getPreferredLanguage(request);
    context.setVariable("preferredLanguage", preferredLanguage);
    context.setVariable("shouldDisplayRtl", LanguageUtils.shouldDisplayRtl(preferredLanguage));
    context.setVariable("enabledLanguages", enabledLanguages());
    context.setVariable("updateLanguageAction", getUpdateLanguageAction(applicantId));
    context.setVariable("redirectUri", getUpdateLanguageRedirectUri(request, profile, applicantId));

    // Add auth parameters.
    boolean isTi = profile.map(CiviFormProfile::isTrustedIntermediary).orElse(false);
    boolean isGuest = applicantPersonalInfo.getType() == GUEST && !isTi;

    context.setVariable("isTrustedIntermediary", isTi);
    context.setVariable("isGuest", isGuest);
    context.setVariable("hasProfile", profile.isPresent());
    context.setVariable("applicantDisplayName", applicantPersonalInfo.getDisplayString(messages));
    context.setVariable("tiDashboardHref", getTiDashboardHref());
    String logoutLink = org.pac4j.play.routes.LogoutController.logout().url();
    context.setVariable("logoutLink", logoutLink);

    // Set branding theme colors.
    context.setVariable("themeColorPrimary", THEME_PRIMARY_HEX);
    context.setVariable("themeColorPrimaryDark", THEME_PRIMARY_DARKER_HEX);
    if (settingsManifest.getCustomThemeColorsEnabled(request)) {
      settingsManifest
          .getThemeColorPrimary(request)
          .filter(setting -> !setting.isEmpty())
          .ifPresent(colorPrimary -> context.setVariable("themeColorPrimary", colorPrimary));
      settingsManifest
          .getThemeColorPrimaryDark(request)
          .filter(setting -> !setting.isEmpty())
          .ifPresent(
              colorPrimaryDark -> context.setVariable("themeColorPrimaryDark", colorPrimaryDark));
    }

    // In Thymeleaf, it's impossible to add escaped text inside unescaped text, which makes it
    // difficult to add HTML within a message. So we have to manually build the html for a link
    // that will be embedded in the guest alert in the header.
    context.setVariable(
        "endSessionLinkHtml",
        "<a id=\"logout-button\" class=\"usa-link\" role=\"button\" href=\""
            + logoutLink
            + "\">"
            + messages.at(MessageKey.END_YOUR_SESSION.getKeyName())
            + "</a>");
    context.setVariable(
        "endSessionLinkAriaLabel", messages.at(MessageKey.END_YOUR_SESSION.getKeyName()));
    context.setVariable("loginLink", routes.LoginController.applicantLogin(Optional.empty()).url());
    if (!isGuest) {
      context.setVariable(
          "loggedInAs", getAccountIdentifier(isTi, profile, applicantPersonalInfo, messages));
    }

    // Default page title
    context.setVariable("pageTitle", messages.at(MessageKey.CONTENT_FIND_PROGRAMS.getKeyName()));

    context.setVariable("isDevOrStaging", isDevOrStaging);

    maybeSetUpNotProductionBanner(context, request, messages);
    boolean sessionTimeoutEnabled = settingsManifest.getSessionTimeoutEnabled(request);
    context.setVariable("sessionTimeoutEnabled", sessionTimeoutEnabled);
    if (sessionTimeoutEnabled) {
      context.setVariable("extendSessionUrl", routes.SessionController.extendSession().url());
    }

    boolean sessionReplayProtectionEnabled = settingsManifest.getSessionReplayProtectionEnabled();
    context.setVariable("sessionReplayProtectionEnabled", sessionReplayProtectionEnabled);
    if (sessionReplayProtectionEnabled) {
      int sessionDurationMinutes = settingsManifest.getMaximumSessionDurationMinutes().get();
      String sessionExpirationBanner =
          messages.at(
              MessageKey.BANNER_SESSION_EXPIRATION.getKeyName(),
              getSessionDurationMessage(sessionDurationMinutes, messages));
      context.setVariable("sessionReplayBanner", sessionExpirationBanner);
    }

    boolean loginDropdownEnabled = settingsManifest.getLoginDropdownEnabled(request);
    context.setVariable("loginDropdownEnabled", loginDropdownEnabled);

    boolean showDebugTools =
        isDevOrStaging && !settingsManifest.getStagingDisableDemoModeLogins(request);
    context.setVariable("showDebugTools", showDebugTools);
    if (showDebugTools) {
      context.setVariable(
          "fakeCiviformAdminUrl",
          routes.CallbackController.fakeAdmin(
                  FakeAdminClient.CLIENT_NAME, FakeAdminClient.GLOBAL_ADMIN)
              .url());
      context.setVariable(
          "fakeProgramAdminUrl",
          routes.CallbackController.fakeAdmin(
                  FakeAdminClient.CLIENT_NAME, FakeAdminClient.PROGRAM_ADMIN)
              .url());
      context.setVariable(
          "fakeDualAdminUrl",
          routes.CallbackController.fakeAdmin(
                  FakeAdminClient.CLIENT_NAME, FakeAdminClient.DUAL_ADMIN)
              .url());
      context.setVariable(
          "fakeTrustedIntermediaryUrl",
          routes.CallbackController.fakeAdmin(
                  FakeAdminClient.CLIENT_NAME, FakeAdminClient.TRUSTED_INTERMEDIARY)
              .url());
      context.setVariable(
          "additionalToolsUrl", controllers.dev.routes.DevToolsController.index().url());
    }

    return context;
  }

  private static String getSessionDurationMessage(int sessionDurationMinutes, Messages messages) {
    int sessionDurationHours = sessionDurationMinutes / 60;
    // The remaining minutes after accounting for whole hours.
    int remainingSessionDurationMinutes = sessionDurationMinutes % 60;
    if (sessionDurationMinutes < 60) {
      return sessionDurationMinutes == 1
          ? messages.at(MessageKey.BANNER_MINUTE.getKeyName())
          : messages.at(
              MessageKey.BANNER_MINUTES.getKeyName(), String.valueOf(sessionDurationMinutes));
    }
    // The session duration is a whole number of hours.
    if (remainingSessionDurationMinutes == 0) {
      return sessionDurationHours == 1
          ? messages.at(MessageKey.BANNER_HOUR.getKeyName())
          : messages.at(MessageKey.BANNER_HOURS.getKeyName(), String.valueOf(sessionDurationHours));
    } else {
      if (sessionDurationHours == 1) {
        return remainingSessionDurationMinutes == 1
            ? messages.at(MessageKey.BANNER_HOUR_AND_MINUTE.getKeyName())
            : messages.at(
                MessageKey.BANNER_HOUR_AND_MINUTES.getKeyName(),
                String.valueOf(remainingSessionDurationMinutes));
      }
      if (remainingSessionDurationMinutes == 1) {
        return messages.at(
            MessageKey.BANNER_HOURS_AND_MINUTE.getKeyName(), String.valueOf(sessionDurationHours));
      }
      return messages.at(
          MessageKey.BANNER_HOURS_AND_MINUTES.getKeyName(),
          String.valueOf(sessionDurationHours),
          String.valueOf(remainingSessionDurationMinutes));
    }
  }

  private String getAccountIdentifier(
      boolean isTi,
      Optional<CiviFormProfile> profile,
      ApplicantPersonalInfo applicantPersonalInfo,
      Messages messages) {
    // For TIs we use the account email rather than first and last name because
    // TIs usually do not have the latter data available, but will always have
    // an email address because they are authenticated.
    if (isTi) {
      // CommonProfile.getEmail() can return null, so we guard that with a generic
      // display string.
      // If it's a TI, there will definitely be a profile.
      String email =
          Optional.ofNullable(profile.get().getProfileData().getEmail())
              .orElse("Trusted Intermediary");

      // To ensure a consistent string with browser snapshots, we override the
      // display email.
      if (email.startsWith("fake-trusted-intermediary") && email.endsWith("@example.com")) {
        return "trusted-intermediary@example.com";
      }

      return email;
    }
    return applicantPersonalInfo.getDisplayString(messages);
  }

  private ImmutableMap<Lang, String> enabledLanguages() {
    return languageUtils.getApplicantEnabledLanguages().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                lang -> lang, lang -> languageUtils.getDisplayString(lang.locale())));
  }

  private String getUpdateLanguageAction(Optional<Long> applicantId) {
    return applicantId.isPresent()
        ? controllers.applicant.routes.ApplicantInformationController.setLangFromSwitcher(
                applicantId.get())
            .url()
        : controllers.applicant.routes.ApplicantInformationController
            .setLangFromSwitcherWithoutApplicant()
            .url();
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

  private void maybeSetUpNotProductionBanner(
      ThymeleafModule.PlayThymeleafContext context, Request request, Messages messages) {
    if (!settingsManifest.getShowNotProductionBannerEnabled(request)) {
      return;
    }
    context.setVariable(
        "showNotProductionBannerEnabled",
        settingsManifest.getShowNotProductionBannerEnabled(request));

    // In Thymeleaf, it's impossible to add escaped text inside unescaped text, which makes it
    // difficult to add HTML within a message. So we have to manually build the html for a link
    // that will be embedded in the banner.
    Optional<String> linkHref = settingsManifest.getCivicEntityProductionUrl(request);
    Optional<String> linkText = settingsManifest.getWhitelabelCivicEntityFullName(request);
    Optional<String> unescapedDescription = Optional.empty();
    if (!linkHref.orElse("").isEmpty() && !linkText.orElse("").isEmpty()) {
      String linkHtml =
          "<a href=\"" + linkHref.get() + "\" class=\"usa-link\">" + linkText.get() + "</a>";
      String rawString = messages.at(MessageKey.NOT_FOR_PRODUCTION_BANNER_LINE_2.getKeyName());
      unescapedDescription = Optional.of(rawString.replace("{0}", linkHtml));
    }
    String alertTitle = messages.at(MessageKey.NOT_FOR_PRODUCTION_BANNER_LINE_1.getKeyName());
    AlertSettings notProductionAlertSettings =
        new AlertSettings(
            true,
            Optional.of(alertTitle),
            unescapedDescription.orElse(""),
            unescapedDescription.isPresent(),
            AlertType.EMERGENCY,
            ImmutableList.of(),
            /* customText= */ Optional.empty(),
            Optional.of(AlertSettings.getTitleAriaLabel(messages, AlertType.EMERGENCY, alertTitle)),
            /* isSlim= */ false);
    context.setVariable("notProductionAlertSettings", notProductionAlertSettings);
  }

  /** Create a page title for a step in the application process. */
  protected String pageTitleWithBlockProgress(
      String programTitle, int blockIndex, int totalBlockCount, Messages messages) {
    // While applicant is filling out the application, include the block they are on as part of
    // their progress.
    blockIndex++;
    // The summary page counts as a step
    totalBlockCount++;
    String blockNumberText =
        messages.at(MessageKey.CONTENT_BLOCK_PROGRESS.getKeyName(), blockIndex, totalBlockCount);
    return String.format("%s — %s", programTitle, blockNumberText);
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
}
