package services;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import controllers.AssetsFinder;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import play.Environment;

/** Wrapper around Play's ${@link AssetsFinder} that handles picking paths from the bundler tool. */
@Singleton
@Slf4j
public final class BundledAssetsFinder {
  @Getter private final boolean useDevServer;
  private final String devServerUrl;
  private final AssetsFinder assetsFinder;

  @Inject
  public BundledAssetsFinder(Config config, Environment environment, AssetsFinder assetsFinder) {
    checkNotNull(config);

    boolean isDev = checkNotNull(environment).isDev();

    this.useDevServer = isDev && config.getBoolean("bundler.useDevServer");
    this.devServerUrl = config.getString("bundler.devServerUrl");
    this.assetsFinder = checkNotNull(assetsFinder);
  }

  /** Get URL for Vite Hot Module Replacement (HMR) client */
  public String viteClientUrl() {
    if (useDevServer) {
      return devServerUrl + "/@vite/client";
    }

    return null;
  }

  /** Get URL for a JavaScript entry point */
  private String vitePath(String entryPoint) {
    if (useDevServer) {
      return devServerUrl + "/" + entryPoint;
    }

    return assetsFinder.path(entryPoint);
  }

  public String path(String path) {
    return assetsFinder.path(path);
  }

  public String getTailwindStylesheet() {
    return vitePath("stylesheets/tailwind.css");
  }

  public String getUswdsStylesheet() {
    return isUseDevServer()
        ? vitePath("app/assets/stylesheets/uswds/styles.scss")
        : vitePath("dist/uswds_css.min.css");
  }

  public String getNorthStarStylesheet() {
    return isUseDevServer()
        ? vitePath("app/assets/stylesheets/northstar/styles.scss")
        : vitePath("dist/northstar_css.min.css");
  }

  public String getMapLibreGLStylesheet() {
    return isUseDevServer()
        ? vitePath("node_modules/maplibre-gl/dist/maplibre-gl.css")
        : vitePath("dist/maplibregl.min.css");
  }

  public String getAdminJsBundle() {
    return isUseDevServer()
        ? vitePath("app/assets/javascripts/admin_entry_point.ts")
        : vitePath("dist/admin.bundle.js");
  }

  public String getApplicantJsBundle() {
    return isUseDevServer()
        ? vitePath("app/assets/javascripts/applicant_entry_point.ts")
        : vitePath("dist/applicant.bundle.js");
  }

  public String getUswdsJsInit() {
    return isUseDevServer()
        ? vitePath("app/assets/javascripts/uswds/uswds-init.min.js")
        : vitePath("javascripts/uswds/uswds-init.min.js");
  }

  public String getUswdsJsBundle() {
    return isUseDevServer()
        ? vitePath("node_modules/@uswds/uswds/dist/js/uswds.min.js")
        : vitePath("dist/uswds_js.bundle.js");
  }

  public String getSwaggerUiCss() {
    return isUseDevServer()
        ? vitePath("node_modules/swagger-ui-dist/swagger-ui.css")
        : vitePath("dist/swagger-ui/swagger-ui.css");
  }

  public String getSwaggerUiJs() {
    return isUseDevServer()
        ? vitePath("node_modules/swagger-ui-dist/swagger-ui-bundle.js")
        : vitePath("dist/swagger-ui/swagger-ui-bundle.js");
  }

  public String getSwaggeruiPresetJs() {
    return isUseDevServer()
        ? vitePath("node_modules/swagger-ui-dist/swagger-ui-standalone-preset.js")
        : vitePath("dist/swagger-ui/swagger-ui-standalone-preset.js");
  }
}
