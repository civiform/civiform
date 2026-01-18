package services;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import controllers.AssetsFinder;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import play.Environment;

/**
 * Wrapper around Play's {@link AssetsFinder} that handles picking paths from the bundler tool.
 *
 * <p>Below methods return the full, resolved URL for assets.
 *
 * <p>When running in dev mode with the bundler web server they point at either our code files or
 * directly to a file in the <strong>node_modules</strong>.
 *
 * <p>When running without the bundler web server they return the URL that points to the
 * pre-compiled bundled asset file resolved by Play's AssetsFinder. The AssetsFinder will
 * automatically add the appropriate hash to the output filename needed for cache busting.
 */
@Singleton
@Slf4j
public final class BundledAssetsFinder {
  private final boolean useDevServer;
  private final String devServerUrl;
  private final AssetsFinder assetsFinder;

  @Inject
  public BundledAssetsFinder(Config config, Environment environment, AssetsFinder assetsFinder) {
    checkNotNull(config);
    checkNotNull(environment);

    this.assetsFinder = checkNotNull(assetsFinder);
    this.useDevServer = environment.isDev() && config.getBoolean("bundler.useDevServer");
    this.devServerUrl = config.getString("bundler.devServerUrl");
  }

  public boolean useBundlerDevServer() {
    return useDevServer;
  }

  /** Get URL for a bundler file entry point */
  private String bundlerPath(String entryPoint) {
    if (useDevServer) {
      return devServerUrl + "/" + entryPoint;
    }

    return assetsFinder.path(entryPoint);
  }

  /** Get the URL for a file from Play's AssetsFinder */
  public String path(String path) {
    return assetsFinder.path(path);
  }

  /** Get URL for Vite Hot Module Replacement (HMR) client */
  public String viteClientUrl() {
    if (useDevServer) {
      return devServerUrl + "/@vite/client";
    }

    return null;
  }

  public String getTailwindStylesheet() {
    return bundlerPath("stylesheets/tailwind.css");
  }

  public String getUswdsStylesheet() {
    return useDevServer
        ? bundlerPath("app/assets/stylesheets/uswds/styles.scss")
        : bundlerPath("dist/uswds_css.min.css");
  }

  public String getNorthStarStylesheet() {
    return useDevServer
        ? bundlerPath("app/assets/stylesheets/northstar/styles.scss")
        : bundlerPath("dist/northstar_css.min.css");
  }

  public String getMapLibreGLStylesheet() {
    return useDevServer
        ? bundlerPath("node_modules/maplibre-gl/dist/maplibre-gl.css")
        : bundlerPath("dist/maplibregl.min.css");
  }

  public String getAdminJsBundle() {
    return useDevServer
        ? bundlerPath("app/assets/javascripts/pages/admin/admin_entry_point.ts")
        : bundlerPath("dist/admin.bundle.js");
  }

  public String getApplicantJsBundle() {
    return useDevServer
        ? bundlerPath("app/assets/javascripts/pages/applicant/applicant_entry_point.ts")
        : bundlerPath("dist/applicant.bundle.js");
  }

  public String getUswdsJsInit() {
    return useDevServer
        ? bundlerPath("node_modules/@uswds/uswds/dist/js/uswds-init.min.js")
        : bundlerPath("dist/uswdsinit_js.bundle.js");
  }

  public String getUswdsJsBundle() {
    return useDevServer
        ? bundlerPath("node_modules/@uswds/uswds/dist/js/uswds.min.js")
        : bundlerPath("dist/uswds_js.bundle.js");
  }

  public String getSwaggerUiCss() {
    return useDevServer
        ? bundlerPath("node_modules/swagger-ui-dist/swagger-ui.css")
        : bundlerPath("dist/swagger-ui/swagger-ui.css");
  }

  public String getSwaggerUiJs() {
    return useDevServer
        ? bundlerPath("node_modules/swagger-ui-dist/swagger-ui-bundle.js")
        : bundlerPath("dist/swagger-ui/swagger-ui-bundle.js");
  }

  public String getSwaggeruiPresetJs() {
    return useDevServer
        ? bundlerPath("node_modules/swagger-ui-dist/swagger-ui-standalone-preset.js")
        : bundlerPath("dist/swagger-ui/swagger-ui-standalone-preset.js");
  }
}
