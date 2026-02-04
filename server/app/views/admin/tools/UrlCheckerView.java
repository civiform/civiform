package views.admin.tools;

import auth.ProfileUtils;
import com.google.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.BundledAssetsFinder;
import services.settings.SettingsManifest;
import views.admin.TransitionalLayoutBaseView;

public class UrlCheckerView extends TransitionalLayoutBaseView<UrlCheckerViewModel> {
  @Inject
  public UrlCheckerView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest,
      BundledAssetsFinder bundledAssetsFinder,
      ProfileUtils profileUtils) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        settingsManifest,
        bundledAssetsFinder,
        profileUtils);
  }

  @Override
  protected String pageTitle() {
    return "URL Checker";
  }

  @Override
  protected String pageTemplate() {
    return "admin/tools/UrlChecker.html";
  }
}
