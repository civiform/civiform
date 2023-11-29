package views.admin;

import com.google.inject.Inject;
import controllers.AssetsFinder;
import services.DeploymentType;
import services.settings.SettingsManifest;
import views.ViewUtils;

public final class AdminLayoutFactory {

  private final ViewUtils viewUtils;
  private final SettingsManifest settingsManifest;
  private final DeploymentType deploymentType;
  private final AssetsFinder assetsFinder;

  @Inject
  public AdminLayoutFactory(
      ViewUtils viewUtils,
      SettingsManifest settingsManifest,
      DeploymentType deploymentType,
      AssetsFinder assetsFinder) {
    this.viewUtils = viewUtils;
    this.settingsManifest = settingsManifest;
    this.deploymentType = deploymentType;
    this.assetsFinder = assetsFinder;
  }

  public AdminLayout getLayout(AdminLayout.NavPage navPage) {
    return new AdminLayout(viewUtils, navPage, settingsManifest, deploymentType, assetsFinder);
  }
}
