package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static services.applicant.ApplicantPersonalInfo.ApplicantType.GUEST;

import auth.CiviFormProfile;
import auth.FakeAdminClient;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import controllers.routes;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Lang;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.AlertSettings;
import services.AlertType;
import services.DeploymentType;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.settings.SettingsManifest;
import views.components.Icons;
import views.html.helper.CSRF;

// TODO(dwaterman)
public abstract class NorthStarBaseView {
  protected final TemplateEngine templateEngine;
  protected final ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory;
  protected final AssetsFinder assetsFinder;
  protected final ApplicantRoutes applicantRoutes;
  protected final SettingsManifest settingsManifest;
  protected final LanguageUtils languageUtils;
  protected final boolean isDevOrStaging;

  protected NorthStarBaseView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes,
      SettingsManifest settingsManifest,
      LanguageUtils languageUtils,
      DeploymentType deploymentType) {
    this.templateEngine = checkNotNull(templateEngine);
    this.playThymeleafContextFactory = checkNotNull(playThymeleafContextFactory);
    this.assetsFinder = checkNotNull(assetsFinder);
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
    context.setVariable("tailwindStylesheet", assetsFinder.path("stylesheets/tailwind.css"));
    context.setVariable("uswdsStylesheet", assetsFinder.path("dist/uswds.min.css"));
    context.setVariable("northStarStylesheet", assetsFinder.path("dist/uswds_northstar.min.css"));
    context.setVariable("applicantJsBundle", assetsFinder.path("dist/applicant.bundle.js"));
    context.setVariable("uswdsJsInit", assetsFinder.path("javascripts/uswds/uswds-init.min.js"));
    context.setVariable("uswdsJsBundle", assetsFinder.path("dist/uswds.bundle.js"));
    context.setVariable("cspNonce", CspUtil.getNonce(request));
    context.setVariable("csrfToken", CSRF.getToken(request.asScala()).value());
    context.setVariable(
        "smallLogoUrl",
        settingsManifest
            .getCivicEntitySmallLogoUrl()
            .orElse(assetsFinder.path("Images/civiform-staging.png")));
    context.setVariable(
        "civicEntityShortName", settingsManifest.getWhitelabelCivicEntityShortName(request).get());
    context.setVariable(
        "civicEntityFullName", settingsManifest.getWhitelabelCivicEntityFullName(request).get());
    context.setVariable("adminLoginUrl", routes.LoginController.adminLogin().url());
    context.setVariable("closeIcon", Icons.CLOSE);
    context.setVariable("httpsIcon", assetsFinder.path("Images/uswds/icon-https.svg"));
    context.setVariable("govIcon", assetsFinder.path("Images/uswds/icon-dot-gov.svg"));

    // Language selector params
    context.setVariable("preferredLanguage", languageUtils.getPreferredLanguage(request));
    context.setVariable("enabledLanguages", enabledLanguages());
    context.setVariable("updateLanguageAction", getUpdateLanguageAction(applicantId));
    context.setVariable("requestUri", request.uri());

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
    context.setVariable("primaryColor", "#ffbe2e");
    // In Thymeleaf, it's impossible to add escaped text inside unescaped text, which makes it
    // difficult to add HTML within a message. So we have to manually build the html for a link
    // that will be embedded in the guest alert in the header.
    context.setVariable(
        "endSessionLinkHtml",
        "<a id=\"logout-button\" class=\"usa-link\" href=\""
            + logoutLink
            + "\">"
            + messages.at(MessageKey.END_YOUR_SESSION.getKeyName())
            + "</a>");
    context.setVariable("loginLink", routes.LoginController.applicantLogin(Optional.empty()).url());
    if (!isGuest) {
      context.setVariable(
          "loggedInAs", getAccountIdentifier(isTi, profile, applicantPersonalInfo, messages));
    }

    // Default page title
    context.setVariable("pageTitle", messages.at(MessageKey.CONTENT_FIND_PROGRAMS.getKeyName()));

    context.setVariable("isDevOrStaging", isDevOrStaging);

    // Set Sass variable
    // probably need to read from sass file
    Optional<String> primaryColor = settingsManifest.getNorthStarUiPrimaryColor(request);
    if (primaryColor.isPresent() && !primaryColor.get().isEmpty()) {
      // TODO(dwaterman): change this; we just need to keep the app from crashing because
      // of the default string value for the primary color setting.
      if (!primaryColor.get().equals("CHANGE ME")) {
        updateScss(primaryColor.get());
      }
    }

    maybeSetUpNotProductionBanner(context, request, messages);

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

    // Other options
    boolean isApplicationExportable = settingsManifest.getApplicationExportable(request);
    context.setVariable("isApplicationExportable", isApplicationExportable);

    return context;
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

  private void updateScss(String primaryColor) {
    System.out.println("updateScss");
    try {
      BufferedReader in =
          Files.newBufferedReader(
              Paths.get(
                  System.getProperty("user.dir"),
                  "app/assets/stylesheets/northstar/_uswds-theme.scss"),
              Charset.defaultCharset());
      String output = "";
      // String newColorVariableSetting = "$primary-color-variable: \"red-cool-60v\";\n";
      String newColorVariableSetting = "$primary-color-variable: " + primaryColor + ";\n";

      String line = in.readLine();
      while (line != null) {
        if (line.contains("$primary-color-variable:")) {
          System.out.println("Writing new variable");
          output += newColorVariableSetting;
        } else {
          System.out.println("Keeping old line");
          output += line;
          output += "\n";
        }
        line = in.readLine();
      }
      in.close();

      BufferedWriter out =
          Files.newBufferedWriter(
              Paths.get(
                  System.getProperty("user.dir"),
                  "app/assets/stylesheets/northstar/_uswds-theme.scss"),
              Charset.defaultCharset());
      out.write(output, 0, output.length());
      out.flush();
      out.close();

    } catch (IOException e) {
      System.out.println("IOException");
      e.printStackTrace();
    }
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

    AlertSettings notProductionAlertSettings =
        new AlertSettings(
            true,
            Optional.of(messages.at(MessageKey.NOT_FOR_PRODUCTION_BANNER_LINE_1.getKeyName())),
            unescapedDescription.orElse(""),
            unescapedDescription.isPresent(),
            AlertType.EMERGENCY,
            ImmutableList.of(),
            /* customText= */ Optional.empty(),
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
