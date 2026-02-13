package views.trustedintermediary;

import auth.ProfileUtils;
import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.BundledAssetsFinder;
import services.settings.SettingsManifest;

public final class TiDashboardPageView
    extends TrustedIntermediaryLayoutBaseView<TiDashboardPageViewModel> {
  @Inject
  public TiDashboardPageView(
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
  protected String pageTitle(TiDashboardPageViewModel model) {
    return model.getTiGroupName();
  }

  @Override
  protected String pageTemplate() {
    return "trustedintermediary/TiDashboardPage.html";
  }
}
