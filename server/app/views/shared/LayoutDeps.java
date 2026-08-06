package views.shared;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import com.google.inject.Inject;
import services.BundledAssetsFinder;

/** Common Layout Dependencies */
public record LayoutDeps(
    BaseViewDeps baseViewDeps, BundledAssetsFinder bundledAssetsFinder, ProfileUtils profileUtils) {
  @Inject
  public LayoutDeps(
      BaseViewDeps baseViewDeps,
      BundledAssetsFinder bundledAssetsFinder,
      ProfileUtils profileUtils) {
    this.baseViewDeps = checkNotNull(baseViewDeps);
    this.bundledAssetsFinder = checkNotNull(bundledAssetsFinder);
    this.profileUtils = checkNotNull(profileUtils);
  }
}
