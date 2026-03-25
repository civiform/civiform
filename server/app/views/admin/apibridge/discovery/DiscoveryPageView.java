package views.admin.apibridge.discovery;

import auth.ProfileUtils;
import com.google.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.BundledAssetsFinder;
import services.settings.SettingsManifest;
import views.admin.AdminLayout;
import views.admin.TransitionalLayoutBaseView;

/** View setup for rendering the DiscoveryPage.html */
public class DiscoveryPageView extends TransitionalLayoutBaseView<DiscoveryPageViewModel> {

  @Inject
  public DiscoveryPageView(
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
  protected String pageTitle(DiscoveryPageViewModel model) {
    return "API Bridge Discovery";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.API_BRIDGE_DISCOVERY;
  }

  @Override
  protected String pageTemplate() {
    return "admin/apibridge/discovery/DiscoveryPage.html";
  }
}
