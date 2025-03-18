package views.admin;

import com.google.inject.Inject;
import controllers.AssetsFinder;
import play.i18n.MessagesApi;
import services.DeploymentType;
import services.TranslationLocales;
import services.settings.SettingsManifest;
import views.ViewUtils;

public final class AdminLayoutFactory {

  private final ViewUtils viewUtils;
  private final SettingsManifest settingsManifest;
  private final TranslationLocales translationLocales;
  private final DeploymentType deploymentType;
  private final AssetsFinder assetsFinder;
  private final MessagesApi messagesApi;

  @Inject
  public AdminLayoutFactory(
      ViewUtils viewUtils,
      SettingsManifest settingsManifest,
      TranslationLocales translationLocales,
      DeploymentType deploymentType,
      AssetsFinder assetsFinder,
      MessagesApi messagesApi) {
    this.viewUtils = viewUtils;
    this.settingsManifest = settingsManifest;
    this.translationLocales = translationLocales;
    this.deploymentType = deploymentType;
    this.assetsFinder = assetsFinder;
    this.messagesApi = messagesApi;
  }

  public AdminLayout getLayout(AdminLayout.NavPage navPage) {
    return new AdminLayout(
        viewUtils,
        navPage,
        settingsManifest,
        translationLocales,
        deploymentType,
        assetsFinder,
        messagesApi);
  }
}
