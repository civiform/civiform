package views.admin;

import com.google.inject.Inject;
import com.typesafe.config.Config;
import featureflags.FeatureFlags;
import services.DeploymentType;
import views.ViewUtils;

public final class AdminLayoutFactory {

  private final ViewUtils viewUtils;
  private final Config configuration;
  private final FeatureFlags featureFlags;
  private final DeploymentType deploymentType;

  @Inject
  public AdminLayoutFactory(
      ViewUtils viewUtils,
      Config configuration,
      FeatureFlags featureFlags,
      DeploymentType deploymentType) {
    this.viewUtils = viewUtils;
    this.configuration = configuration;
    this.featureFlags = featureFlags;
    this.deploymentType = deploymentType;
  }

  public AdminLayout getLayout(AdminLayout.NavPage navPage) {
    return new AdminLayout(viewUtils, configuration, navPage, featureFlags, deploymentType);
  }
}
