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
   * Generates an HTML script tag for loading the javascript file found at
   * public/javascripts/[filename].js.
   */
  public Tag makeLocalJsTag(String filename) {
    return script()
        .withSrc(assetsFinder.path("javascripts/" + filename + ".js"))
        .withType("text/javascript");
  }

    /**
   * Generates an HTML script tag for loading the javascript file found at
   * public/javascripts/[filename].js.
   */
  public Tag makeCdnJsTag(String cdnUrl) {
    return script().withSrc(cdnUrl).withType("text/javascript");
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
