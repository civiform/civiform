package views.admin.reporting;

import auth.ProfileUtils;
import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.BundledAssetsFinder;
import services.settings.SettingsManifest;
import views.admin.AdminLayout;
import views.admin.AdminLayoutBaseView;

public final class AdminReportingProgramPageView
    extends AdminLayoutBaseView<AdminReportingProgramPageViewModel> {
  @Inject
  public AdminReportingProgramPageView(
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
  protected String pageTitle(AdminReportingProgramPageViewModel model) {
    return "Reporting";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.REPORTING;
  }

  @Override
  protected String pageTemplate() {
    return "admin/reporting/AdminReportingProgramPage.html";
  }
}
