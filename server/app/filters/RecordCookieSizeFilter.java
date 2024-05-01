package filters;

import io.prometheus.client.Histogram;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Http;
import play.routing.HandlerDef;
import play.routing.Router;

/**
 * Filter that exports the size of the PLAY_SESSION cookie to prometheus, parameterized by
 * controller method.
 */
public final class RecordCookieSizeFilter extends EssentialFilter {
  public static int BUCKET_SIZE = 512;
  public static int NUM_BUCKETS = 10;

  private Histogram PLAY_SESSION_COOKIE_SIZE =
      Histogram.build()
          .name("play_session_cookie_size_bytes")
          .linearBuckets(0, BUCKET_SIZE, NUM_BUCKETS)
          .labelNames("controller_method")
          .help("Size of the PLAY_SESSION cookie in bytes")
          .register();

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        requestHeader -> {
          String controllerMethod = getControllerMethod(requestHeader);
          requestHeader
              .getCookie("PLAY_SESSION")
              .ifPresent(
                  cookie -> {
                    PLAY_SESSION_COOKIE_SIZE
                        .labels(controllerMethod)
                        .observe(cookie.value().length());
                  });
          return next.apply(requestHeader);
        });
  }

  private String getControllerMethod(Http.RequestHeader requestHeader) {
    // Not always present in tests.
    if (requestHeader.attrs().containsKey(Router.Attrs.HANDLER_DEF)) {
      HandlerDef handlerDef = requestHeader.attrs().get(Router.Attrs.HANDLER_DEF);
      return String.format("%s.%s", handlerDef.controller(), handlerDef.method());
    }
    return "unknown";
  }
}
