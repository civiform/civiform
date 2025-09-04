package views.admin.apibridge.discovery;

import auth.ProfileUtils;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.settings.SettingsManifest;
import views.admin.AdminLayout;
import views.admin.AdminLayoutBaseView;

/** View setup for rendering the DiscoveryPage.html */
public class DiscoveryPageView extends AdminLayoutBaseView<DiscoveryPageViewModel> {

  @Inject
  public DiscoveryPageView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ProfileUtils profileUtils,
      SettingsManifest settingsManifest) {
    super(
        templateEngine, playThymeleafContextFactory, settingsManifest, assetsFinder, profileUtils);
  }

  @Override
  protected String pageTitle() {
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
