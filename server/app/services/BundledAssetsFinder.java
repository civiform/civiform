package services;

import static com.google.common.base.Preconditions.checkNotNull;

import controllers.AssetsFinder;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/** Wrapper around Play's ${@link AssetsFinder} that handles picking paths from the bundler tool. */
@Singleton
@Slf4j
public final class BundledAssetsFinder {
  private final AssetsFinder assetsFinder;

  @Inject
  public BundledAssetsFinder(AssetsFinder assetsFinder) {
    this.assetsFinder = checkNotNull(assetsFinder);
  }

  public String path(String path) {
    return assetsFinder.path(path);
  }

  public String getTailwindStylesheet() {
    return path("stylesheets/tailwind.css");
  }

  public String getUswdsStylesheet() {
    return path("dist/uswds.min.css");
  }

  public String getNorthStarStylesheet() {
    return path("dist/uswds_northstar.min.css");
  }

  public String getMapLibreGLStylesheet() {
    return path("dist/maplibregl.min.css");
  }

  public String getAdminJsBundle() {
    return path("dist/admin.bundle.js");
  }

  public String getApplicantJsBundle() {
    return path("dist/applicant.bundle.js");
  }

  public String getUswdsJsInit() {
    return path("javascripts/uswds/uswds-init.min.js");
  }

  public String getUswdsJsBundle() {
    return path("dist/uswds.bundle.js");
  }
}
