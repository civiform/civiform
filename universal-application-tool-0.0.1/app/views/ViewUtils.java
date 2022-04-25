package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.img;
import static j2html.TagCreator.link;
import static j2html.TagCreator.script;

import controllers.AssetsFinder;
import j2html.tags.Tag;
import javax.inject.Inject;

/** Utility class for accessing stateful view dependencies. */
public final class ViewUtils {
  private final AssetsFinder assetsFinder;

  @Inject
  ViewUtils(AssetsFinder assetsFinder) {
    this.assetsFinder = checkNotNull(assetsFinder);
  }

  /**
   * Generates an HTML script tag for loading the Azure Blob Storage client library from the
   * jsdelivr.net CDN. TOOD(https://github.com/seattle-uat/civiform/issues/2349): Stop using this.
   */
  public Tag makeAzureBlobStoreScriptTag() {
    return script()
        .withSrc("https://cdn.jsdelivr.net/npm/@azure/storage-blob@10.5.0")
        .withType("text/javascript")
        .attr("crossorigin", "anonymous")
        .attr("integrity", "sha256-VFdCcG0JBuOTN0p15rwVT5EIuL7bzWMYi4aD6KeDqus=");
  }

  /**
   * Generates an HTML script tag for loading the javascript file found at
   * public/javascripts/[filename].js.
   */
  public Tag makeLocalJsTag(String filename) {
    return script()
        .withSrc(assetsFinder.path("javascripts/" + filename + ".js"))
        .withType("text/javascript");
  }

  /**
   * Generates a script tag for loading a javascript asset that is provided by a web JAR and found
   * at the given asset route.
   */
  public Tag makeWebJarsTag(String assetsRoute) {
    return script().withSrc(assetsFinder.path(assetsRoute));
  }

  /**
   * Generates an HTML link tag for loading the CSS file found at public/stylesheets/[filename].css.
   */
  Tag makeLocalCssTag(String filename) {
    return link()
        .withHref(assetsFinder.path("stylesheets/" + filename + ".css"))
        .withRel("stylesheet");
  }

  public Tag makeLocalImageTag(String filename) {
    return img().withSrc(assetsFinder.path("Images/" + filename + ".png"));
  }
}
