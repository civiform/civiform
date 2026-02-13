package views.admin.settings;

import auth.ProfileUtils;
import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.BundledAssetsFinder;
import services.settings.SettingsManifest;
import views.admin.AdminLayout;
import views.admin.AdminLayoutBaseView;
import views.admin.LayoutType;

public final class AdminSettingsPageView extends AdminLayoutBaseView<AdminSettingsPageViewModel> {

  @Inject
  public AdminSettingsPageView(
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
  protected String pageTitle(AdminSettingsPageViewModel model) {
    return "Settings";
  }

  @Override
  protected LayoutType layoutType() {
    return LayoutType.CONTENT_WITH_LEFT_SIDEBAR;
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.SETTINGS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/settings/AdminSettingsPage.html";
  }
}
