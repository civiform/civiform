package views.admin.apikeys;

import auth.ProfileUtils;
import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.BundledAssetsFinder;
import services.settings.SettingsManifest;
import views.admin.AdminLayout;
import views.admin.AdminLayoutBaseView;

public final class ApiKeyNewOnePageView extends AdminLayoutBaseView<ApiKeyNewOnePageViewModel> {

  @Inject
  public ApiKeyNewOnePageView(
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
  protected String pageTitle(ApiKeyNewOnePageViewModel model) {
    return "Create a new API key";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.API_KEYS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/apikeys/ApiKeyNewOnePage.html";
  }
}
