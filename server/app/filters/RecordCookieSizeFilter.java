package filters;

import io.prometheus.client.Histogram;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.routing.HandlerDef;
import play.routing.Router;

public class RecordCookieSizeFilter extends EssentialFilter {
  private Histogram PLAY_SESSION_COOKIE_SIZE =
    Histogram.build()
      .name("play_session_cookie_size_bytes")
      .linearBuckets(0, 4096, 512)
      .labelNames("action_method")
      .help("Size of the PLAY_SESSION cookie in bytes")
      .register();

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
      requestHeader -> {
        HandlerDef handlerDef = requestHeader.attrs().get(Router.Attrs.HANDLER_DEF);
        String actionMethod = handlerDef.controller() + "." + handlerDef.method();
        requestHeader.getCookie("PLAY_SESSION").ifPresent(cookie -> {
          PLAY_SESSION_COOKIE_SIZE.labels(actionMethod).observe(cookie.value().length());
        });
        return next.apply(requestHeader);
      }
    );
  }
}
