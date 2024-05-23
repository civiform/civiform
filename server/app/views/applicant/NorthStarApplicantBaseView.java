package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static services.applicant.ApplicantPersonalInfo.ApplicantType.GUEST;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableMap;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import controllers.routes;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Lang;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.applicant.ApplicantPersonalInfo;
import services.settings.SettingsManifest;
import views.components.Icons;
import views.html.helper.CSRF;

public abstract class NorthStarApplicantBaseView {
  protected final TemplateEngine templateEngine;
  protected final ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory;
  protected final AssetsFinder assetsFinder;
  protected final ApplicantRoutes applicantRoutes;
  protected final SettingsManifest settingsManifest;
  protected final LanguageUtils languageUtils;

  NorthStarApplicantBaseView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes,
      SettingsManifest settingsManifest,
      LanguageUtils languageUtils) {
    this.templateEngine = checkNotNull(templateEngine);
    this.playThymeleafContextFactory = checkNotNull(playThymeleafContextFactory);
    this.assetsFinder = checkNotNull(assetsFinder);
    this.applicantRoutes = checkNotNull(applicantRoutes);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.languageUtils = checkNotNull(languageUtils);
  }

  protected ThymeleafModule.PlayThymeleafContext createThymeleafContext(
      Request request,
      Long applicantId,
      CiviFormProfile profile,
      ApplicantPersonalInfo applicantPersonalInfo,
      Messages messages) {
    ThymeleafModule.PlayThymeleafContext context = playThymeleafContextFactory.create(request);
    context.setVariable("tailwindStylesheet", assetsFinder.path("stylesheets/tailwind.css"));
    context.setVariable("uswdsStylesheet", assetsFinder.path("dist/uswds.min.css"));
    context.setVariable("applicantJsBundle", assetsFinder.path("dist/applicant.bundle.js"));
    context.setVariable("uswdsJsInit", assetsFinder.path("javascripts/uswds/uswds-init.min.js"));
    context.setVariable("uswdsJsBundle", assetsFinder.path("dist/uswds.bundle.js"));
    context.setVariable("csrfToken", CSRF.getToken(request.asScala()).value());
    context.setVariable(
        "smallLogoUrl",
        settingsManifest
            .getCivicEntitySmallLogoUrl()
            .orElse(assetsFinder.path("Images/civiform-staging.png")));
    context.setVariable(
        "civicEntityShortName", settingsManifest.getWhitelabelCivicEntityShortName(request).get());
    context.setVariable("closeIcon", Icons.CLOSE);

    // Language selector params
    context.setVariable("preferredLanguage", languageUtils.getPreferredLanguage(request));
    context.setVariable("enabledLanguages", enabledLanguages());
    context.setVariable("updateLanguageAction", getUpdateLanguageAction(applicantId));
    context.setVariable("requestUri", request.uri());

    // Add auth parameters.
    boolean isTi = profile.isTrustedIntermediary();
    boolean isGuest = applicantPersonalInfo.getType() == GUEST && !isTi;

    context.setVariable("isGuest", isGuest);
    context.setVariable("endSessionLink", org.pac4j.play.routes.LogoutController.logout().url());
    context.setVariable("loginLink", routes.LoginController.applicantLogin(Optional.empty()).url());
    if (!isGuest) {
      context.setVariable(
          "loggedInAs", getAccountIdentifier(isTi, profile, applicantPersonalInfo, messages));
    }
    return context;
  }

  private String getAccountIdentifier(
      boolean isTi,
      CiviFormProfile profile,
      ApplicantPersonalInfo applicantPersonalInfo,
      Messages messages) {
    // For TIs we use the account email rather than first and last name because
    // TIs usually do not have the latter data available, but will always have
    // an email address because they are authenticated.
    if (isTi) {
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
    return applicantPersonalInfo.getDisplayString(messages);
  }

  private ImmutableMap<Lang, String> enabledLanguages() {
    return languageUtils.getApplicantEnabledLanguages().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                lang -> lang, lang -> languageUtils.getDisplayString(lang.locale())));
  }

  private String getUpdateLanguageAction(Long applicantId) {
    return controllers.applicant.routes.ApplicantInformationController.setLangFromSwitcher(
            applicantId)
        .url();
  }
}
