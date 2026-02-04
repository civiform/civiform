package views.admin;

import com.google.inject.Inject;
import play.i18n.MessagesApi;
import services.BundledAssetsFinder;
import services.DeploymentType;
import services.TranslationLocales;
import services.settings.SettingsManifest;
import views.ViewUtils;

public final class AdminLayoutFactory {

  private final ViewUtils viewUtils;
  private final SettingsManifest settingsManifest;
  private final TranslationLocales translationLocales;
  private final DeploymentType deploymentType;
  private final BundledAssetsFinder bundledAssetsFinder;
  private final MessagesApi messagesApi;

  @Inject
  public AdminLayoutFactory(
      ViewUtils viewUtils,
      SettingsManifest settingsManifest,
      TranslationLocales translationLocales,
      DeploymentType deploymentType,
      BundledAssetsFinder bundledAssetsFinder,
      MessagesApi messagesApi) {
    this.viewUtils = viewUtils;
    this.settingsManifest = settingsManifest;
    this.translationLocales = translationLocales;
    this.deploymentType = deploymentType;
    this.bundledAssetsFinder = bundledAssetsFinder;
    this.messagesApi = messagesApi;
  }

  public AdminLayout getLayout(AdminLayout.NavPage navPage) {
    return new AdminLayout(
        viewUtils,
        navPage,
        settingsManifest,
        translationLocales,
        deploymentType,
        bundledAssetsFinder,
        messagesApi);
  }
}
