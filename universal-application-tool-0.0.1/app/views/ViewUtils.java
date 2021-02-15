package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.input;
import static j2html.TagCreator.script;

import controllers.AssetsFinder;
import j2html.tags.Tag;
import javax.inject.Inject;
import play.libs.crypto.DefaultCSRFTokenSigner;

/** Utility class for accessing stateful view dependenices. */
final class ViewUtils {

  private final DefaultCSRFTokenSigner tokenSigner;
  private final AssetsFinder assetsFinder;

  @Inject
  ViewUtils(DefaultCSRFTokenSigner tokenSigner, AssetsFinder assetsFinder) {
    this.tokenSigner = checkNotNull(tokenSigner);
    this.assetsFinder = checkNotNull(assetsFinder);
  }

  /** Generates a hidden HTML input tag containing a signed CSRF token. */
  Tag makeCsrfTokenInputTag() {
    return input().isHidden().withValue(tokenSigner.generateSignedToken()).withName("csrfToken");
  }

  /**
   * Generates an HTML script tag for loading the javascript file found at
   * public/javascripts/[filename].js.
   */
  Tag makeLocalJsTag(String filename) {
    return script().withSrc(assetsFinder.path("javascripts/" + filename + ".js"));
  }
}
