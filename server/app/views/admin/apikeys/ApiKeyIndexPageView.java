package views.admin.apikeys;

import auth.ProfileUtils;
import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.BundledAssetsFinder;
import services.settings.SettingsManifest;
import views.admin.AdminLayout;
import views.admin.AdminLayoutBaseView;

public final class ApiKeyIndexPageView extends AdminLayoutBaseView<ApiKeyIndexPageViewModel> {

  @Inject
  public ApiKeyIndexPageView(
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
  protected String pageTitle(ApiKeyIndexPageViewModel model) {
    return "API Keys";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.API_KEYS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/apikeys/ApiKeyIndexPage.html";
  }
}
