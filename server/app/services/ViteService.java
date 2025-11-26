package services;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import controllers.AssetsFinder;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import play.Environment;

@Singleton
@Slf4j
public class ViteService {
  @Getter private final boolean isViteEnabled;
  private final String viteDevServerUrl;
  private final AssetsFinder assetsFinder;

  /*

    if (viteService.isViteEnabled()) {
      context.setVariable("tailwindStylesheet", viteService.path("stylesheets/tailwind.css"));
      context.setVariable("northStarStylesheet", viteService.cssUrl("northstar/styles.scss"));
      context.setVariable("mapLibreGLStylesheet", viteService.cssUrl("maplibre-gl.css"));
      context.setVariable("applicantJsBundle", viteService.jsUrl("applicant_entry_point.ts"));
      context.setVariable("uswdsJsInit", viteService.jsUrl("uswds/uswds-init.min.js"));
      context.setVariable("uswdsJsBundle", viteService.jsUrlFullPath("node_modules/@uswds/uswds/dist/js/uswds.min.js"));
    } else {
      context.setVariable("tailwindStylesheet", viteService.path("stylesheets/tailwind.css"));
      context.setVariable("northStarStylesheet", viteService.cssUrl("dist/northstar_css.min.css"));
      context.setVariable("mapLibreGLStylesheet", viteService.cssUrl("dist/maplibregl.min.css"));
      context.setVariable("applicantJsBundle", viteService.jsUrl("dist/applicant.bundle.js"));
      context.setVariable("uswdsJsInit", viteService.jsUrl("javascripts/uswds/uswds-init.min.js"));
      context.setVariable("uswdsJsBundle", viteService.jsUrlFullPath("dist/uswds_js.bundle.js"));
    }

  */

  @Inject
  public ViteService(Config config, Environment environment, AssetsFinder assetsFinder) {
    checkNotNull(config);

    boolean isDev = checkNotNull(environment).isDev();

    this.isViteEnabled = isDev && config.getBoolean("vite.enabled");
    this.viteDevServerUrl = config.getString("vite.devServerUrl");
    this.assetsFinder = checkNotNull(assetsFinder);
  }

  /**
   * Get URL for a JavaScript entry point
   *
   * @param entryPoint - e.g., "applicant_entry_point" or "admin_entry_point"
   */
  public String jsUrl(String entryPoint) {
    if (isViteEnabled) {
      return viteDevServerUrl + "/app/assets/javascripts/" + entryPoint;
    }
    // Use AssetsFinder to get the asset path
    return assetsFinder.path(entryPoint);
  }

  public String jsUrlFullPath(String entryPoint) {
    if (isViteEnabled) {
      return viteDevServerUrl + "/" + entryPoint;
    }
    // Use AssetsFinder to get the asset path
    return assetsFinder.path(entryPoint);
  }

  /**
   * Get URL for a CSS file
   *
   * @param cssPath - e.g., "styles.css" or "uswds/styles.scss"
   */
  public String cssUrl(String cssPath) {
    if (isViteEnabled) {
      return viteDevServerUrl + "/app/assets/stylesheets/" + cssPath;
    }

    return assetsFinder.path(cssPath);
    //    // Extract filename without extension for production build
    //    String filename = cssPath.contains("/")
    //      ? cssPath.substring(cssPath.lastIndexOf("/") + 1)
    //      : cssPath;
    //    filename = filename.replaceAll("\\.(css|scss)$", "");
    //    return assetsFinder.path("dist/" + filename + ".min.css");
  }

  /** Get URL for Vite Hot Module Replacement (HMR) client */
  public String viteClientUrl() {
    if (isViteEnabled) {
      return viteDevServerUrl + "/@vite/client";
    }

    return null;
  }

  /**
   * Get URL for any asset path
   *
   * @param path - relative path from assets directory
   */
  public String path(String path) {
    if (isViteEnabled) {
      return viteDevServerUrl + "/" + path;
    }
    return assetsFinder.path("dist/" + path);
  }

  public String getTailwindStylesheet() {
    return path("stylesheets/tailwind.css");
  }

  public String getUswdsStylesheet() {
    return isViteEnabled() ? cssUrl("uswds/styles.scss") : cssUrl("dist/uswds_css.min.css");
  }

  public String getNorthStarStylesheet() {
    return isViteEnabled() ? cssUrl("northstar/styles.scss") : cssUrl("dist/northstar_css.min.css");
  }

  public String getMapLibreGLStylesheet() {
    return isViteEnabled() ? cssUrl("maplibre-gl.css") : cssUrl("dist/maplibregl_css.min.css");
  }

  public String getAdminJsBundle() {
    return isViteEnabled() ? jsUrl("admin_entry_point.ts") : jsUrl("dist/admin.bundle.js");
  }

  public String getApplicantJsBundle() {
    return isViteEnabled() ? jsUrl("applicant_entry_point.ts") : jsUrl("dist/applicant.bundle.js");
  }

  public String getUswdsJsInit() {
    return isViteEnabled()
        ? jsUrl("uswds/uswds-init.min.js")
        : jsUrl("javascripts/uswds/uswds-init.min.js");
  }

  public String getUswdsJsBundle() {
    return isViteEnabled()
        ? jsUrlFullPath("node_modules/@uswds/uswds/dist/js/uswds.min.js")
        : jsUrlFullPath("dist/uswds_js.bundle.js");
  }
}
