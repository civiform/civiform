package views.trustedintermediary;

import auth.ProfileUtils;
import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.BundledAssetsFinder;
import services.settings.SettingsManifest;

public final class TiAccountSettingsPageView
    extends TrustedIntermediaryLayoutBaseView<TiAccountSettingsPageViewModel> {
  @Inject
  public TiAccountSettingsPageView(
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
  protected String pageTitle(TiAccountSettingsPageViewModel model) {
    return "Account Settings";
  }

  @Override
  protected String pageHeading(TiAccountSettingsPageViewModel model) {
    return model.getTiGroupName();
  }

  @Override
  protected String pageTemplate() {
    return "trustedintermediary/TiAccountSettingsPage.html";
  }
}
