package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.head;
import static j2html.TagCreator.link;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.script;
import static j2html.TagCreator.title;

import controllers.AssetsFinder;
import j2html.tags.Tag;
import javax.inject.Inject;

/** Utility class for accessing stateful view dependencies. */
public final class ViewUtils {
  private final AssetsFinder assetsFinder;

  public static final String POST = "post";

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

  public Tag makeHead(String cssfile, String trackingTag, String... titlestr) {

    String thetitle = titlestr.length > 0 ? titlestr[0] : "";
    String GA_CODE =
        "window.dataLayer = window.dataLayer || []; function gtag() { dataLayer.push(arguments); }"
            + " gtag('js', new Date()); gtag('config', '%s');";

    return head(
        title(thetitle),
        link().withHref(assetsFinder.path("stylesheets/" + cssfile + ".css")).withRel("stylesheet"),
        script()
            .withSrc("https://www.googletagmanager.com/gtag/js?id=" + trackingTag)
            .attr("async", "true")
            .withType("text/javascript"),
        script(rawHtml(String.format(GA_CODE, trackingTag))).withType("text/javascript"));
  }

  /**
   * Generates an HTML link tag for loading the CSS file found at public/stylesheets/[filename].css.
   */
  Tag makeLocalCssTag(String filename) {
    return link()
        .withHref(assetsFinder.path("stylesheets/" + filename + ".css"))
        .withRel("stylesheet");
  }
}
