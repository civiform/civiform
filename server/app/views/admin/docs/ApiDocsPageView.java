package views.admin.docs;

import auth.ProfileUtils;
import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.BundledAssetsFinder;
import services.settings.SettingsManifest;
import views.admin.AdminLayout;
import views.admin.AdminLayoutBaseView;

public final class ApiDocsPageView extends AdminLayoutBaseView<ApiDocsPageViewModel> {

  @Inject
  public ApiDocsPageView(
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
  protected String pageTitle(ApiDocsPageViewModel model) {
    return "API documentation";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.API_DOCS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/docs/ApiDocsPage.html";
  }
}
