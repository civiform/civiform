package filters;

import java.util.Optional;
import java.util.regex.Pattern;
import play.libs.streams.Accumulator;
import play.mvc.Call;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Http;
import play.mvc.Results;

/**
 * Filter that redirects requests from unsupported browsers (e.g. IE11) to a page that tells user to
 * use newer browser.
 */
public final class UnsupportedBrowserFilter extends EssentialFilter {
  // Trident/[5-7].0 browsers are IE9-IE11.
  private static final Pattern IE_PATTERN = Pattern.compile(" Trident/\\d");

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request -> {
          if (UnsupportedBrowserFilter.isUnsupportedBrowser(request)) {
            Call unsupportedBrowser =
                controllers.routes.SupportController.handleUnsupportedBrowser();
            // Don't redirect if request is for a static asset or for the unsupported browser page
            // itself.
            if (!request.path().startsWith("/assets")
                && !request.path().startsWith(unsupportedBrowser.path())) {
              return Accumulator.done(Results.redirect(unsupportedBrowser));
            }
          }
          return next.apply(request);
        });
  }

  private static boolean isUnsupportedBrowser(Http.RequestHeader request) {
    Optional<String> userAgent = request.header(Http.HeaderNames.USER_AGENT);
    // Disallow IE only. Everything else consider supported.
    return userAgent.map(ua -> IE_PATTERN.matcher(ua).find()).orElse(false);
  }
}
