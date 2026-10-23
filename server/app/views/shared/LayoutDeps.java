package views.shared;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import com.google.inject.Inject;
import services.BundledAssetsFinder;
import services.DeploymentType;

/** Common Layout Dependencies */
public record LayoutDeps(
    BaseViewDeps baseViewDeps,
    BundledAssetsFinder bundledAssetsFinder,
    ProfileUtils profileUtils,
    DeploymentType deploymentType) {
  @Inject
  public LayoutDeps(
      BaseViewDeps baseViewDeps,
      BundledAssetsFinder bundledAssetsFinder,
      ProfileUtils profileUtils,
      DeploymentType deploymentType) {
    this.baseViewDeps = checkNotNull(baseViewDeps);
    this.bundledAssetsFinder = checkNotNull(bundledAssetsFinder);
    this.profileUtils = checkNotNull(profileUtils);
    this.deploymentType = checkNotNull(deploymentType);
  }
}
