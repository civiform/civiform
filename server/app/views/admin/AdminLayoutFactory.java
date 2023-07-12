package views.admin;

import com.google.inject.Inject;
import services.DeploymentType;
import services.settings.SettingsManifest;
import views.ViewUtils;

public final class AdminLayoutFactory {

  private final ViewUtils viewUtils;
  private final SettingsManifest settingsManifest;
  private final DeploymentType deploymentType;

  @Inject
  public AdminLayoutFactory(
      ViewUtils viewUtils, SettingsManifest settingsManifest, DeploymentType deploymentType) {
    this.viewUtils = viewUtils;
    this.settingsManifest = settingsManifest;
    this.deploymentType = deploymentType;
  }

  public AdminLayout getLayout(AdminLayout.NavPage navPage) {
    return new AdminLayout(viewUtils, navPage, settingsManifest, deploymentType);
  }
}
