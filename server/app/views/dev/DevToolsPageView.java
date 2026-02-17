package views.dev;

import auth.ProfileUtils;
import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http;
import services.BundledAssetsFinder;
import services.settings.SettingsManifest;
import views.admin.AdminLayout;
import views.admin.AdminLayoutBaseView;
import views.admin.shared.AdminCommonHeader;

public final class DevToolsPageView extends AdminLayoutBaseView<DevToolsPageViewModel> {
  @Inject
  public DevToolsPageView(
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
  protected void customizeContext(
      Http.Request request, ThymeleafModule.PlayThymeleafContext context) {
    // Avoid requiring a user profile for dev tools page.
    context.setVariable(
        "adminCommonHeader",
        AdminCommonHeader.builder()
            .activeNavPage(AdminLayout.NavPage.NULL_PAGE)
            .isOnlyProgramAdmin(false)
            .isApiBridgeEnabled(settingsManifest.getApiBridgeEnabled(request))
            .build());
  }

  @Override
  protected String pageTitle(DevToolsPageViewModel model) {
    return "Dev Tools";
  }

  @Override
  protected String pageTemplate() {
    return "dev/DevToolsPage.html";
  }
}
