package views.trustedintermediary;

import auth.ProfileUtils;
import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.BundledAssetsFinder;
import services.settings.SettingsManifest;

public final class TiEditClientPageView
    extends TrustedIntermediaryLayoutBaseView<TiEditClientPageViewModel> {
  @Inject
  public TiEditClientPageView(
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
  protected String pageTitle(TiEditClientPageViewModel model) {
    return model.isEdit() ? "Edit Client" : "Add Client";
  }

  @Override
  protected String pageHeading(TiEditClientPageViewModel model) {
    return model.getTiGroupName();
  }

  @Override
  protected String pageTemplate() {
    return "trustedintermediary/TiEditClientPage.html";
  }
}
