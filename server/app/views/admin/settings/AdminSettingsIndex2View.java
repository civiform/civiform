package views.admin.settings;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http;
import services.DeploymentType;
import services.settings.SettingDescription;
import services.settings.SettingsManifest;
import services.settings.SettingsSection;
import views.CspUtil;
import views.NorthStarBaseView;
import views.components.TextFormatter;
import views.html.helper.CSRF;
import java.util.Optional;

import static org.checkerframework.errorprone.com.google.common.base.Preconditions.checkNotNull;

public class AdminSettingsIndex2View extends NorthStarBaseView {
  private static final ImmutableList<String> SECTIONS =
    ImmutableList.of(
      "Feature Flags",
      "Branding",
      "Custom Text",
      "Email Addresses",
      "Data Export API",
      "Observability",
      "External Services",
      "Miscellaneous");

  @Inject
  public AdminSettingsIndex2View(
    TemplateEngine templateEngine,
    ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
    AssetsFinder assetsFinder,
    ApplicantRoutes applicantRoutes,
    SettingsManifest settingsManifest,
    LanguageUtils languageUtils,
    DeploymentType deploymentType) {
    super(
      templateEngine,
      playThymeleafContextFactory,
      assetsFinder,
      applicantRoutes,
      settingsManifest,
      languageUtils,
      deploymentType);
  }

  Http.Request request;

  public String render(Http.Request request) {
    this.request = request;

    ImmutableList<SettingsSection> orderedSections = SECTIONS.stream()
      .map(sectionName -> settingsManifest.getSections().get(sectionName))
      .collect(ImmutableList.toImmutableList());

    ThymeleafModule.PlayThymeleafContext context = playThymeleafContextFactory.create(request);
    context.setVariable("uswdsStylesheet", assetsFinder.path("dist/uswds.min.css"));
    context.setVariable("uswdsJsInit", assetsFinder.path("javascripts/uswds/uswds-init.min.js"));
    context.setVariable("uswdsJsBundle", assetsFinder.path("dist/uswds.bundle.js"));
    context.setVariable("cspNonce", CspUtil.getNonce(request));
    context.setVariable("csrfToken", CSRF.getToken(request.asScala()).value());
    context.setVariable("settingsManifest", settingsManifest);
    context.setVariable("orderedSections", orderedSections);

    // Pass this in so you can reference more complex methods from within the thymeleaf.
    // If memory servers this is akin to the Model-View-Presenter pattern.
    context.setVariable("presenter", this);

    return templateEngine.process("admin/settings/AdminSettingsIndexTemplate", context);
  }


  public String getSettingDisplayValue(SettingDescription settingDescription) {
    checkNotNull(request, "`request` was null. Don't call this before calling the `render` method.");
    var result = settingsManifest.getSettingDisplayValue(request, settingDescription);

    return result.orElse("");
  }

  public String sanitizeHtml(String markdown){
    return TextFormatter.formatTextToSanitizedHTML(markdown, false);
  }
}
