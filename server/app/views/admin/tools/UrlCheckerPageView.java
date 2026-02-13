package views.admin.tools;

import auth.ProfileUtils;
import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.BundledAssetsFinder;
import services.settings.SettingsManifest;
import views.admin.AdminLayoutBaseView;

public final class UrlCheckerPageView extends AdminLayoutBaseView<UrlCheckerViewModel> {
  @Inject
  public UrlCheckerPageView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      BundledAssetsFinder bundledAssetsFinder,
      ProfileUtils profileUtils,
      SettingsManifest settingsManifest) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        settingsManifest,
        bundledAssetsFinder,
        profileUtils);
  }

  @Override
  protected String pageTitle(UrlCheckerViewModel model) {
    return "URL Checker";
  }

  @Override
  protected String pageTemplate() {
    return "admin/tools/UrlChecker.html";
  }
}
