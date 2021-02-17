package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.input;
import static j2html.TagCreator.script;

import controllers.AssetsFinder;
import j2html.tags.Tag;
import javax.inject.Inject;
import play.mvc.Http.Request;
import views.html.helper.CSRF;

/** Utility class for accessing stateful view dependencies. */
final class ViewUtils {
  private final AssetsFinder assetsFinder;

  @Inject
  ViewUtils(AssetsFinder assetsFinder) {
    this.assetsFinder = checkNotNull(assetsFinder);
  }

  /**
   * Generates a hidden HTML input tag containing a signed CSRF token. The token and tag must be
   * present in all UAT forms.
   */
  Tag makeCsrfTokenInputTag(Request request) {
    return input().isHidden().withValue(getCsrfToken(request)).withName("csrfToken");
  }

  private String getCsrfToken(Request request) {
    return CSRF.getToken(request.asScala()).value();
  }

  /**
   * Generates an HTML script tag for loading the javascript file found at
   * public/javascripts/[filename].js.
   */
  Tag makeLocalJsTag(String filename) {
    return script()
        .withSrc(assetsFinder.path("javascripts/" + filename + ".js"))
        .withType("text/javascript");
  }
}
