package filters;

import akka.stream.Materializer;
import io.prometheus.client.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Filter;
import play.mvc.Http;
import play.mvc.Result;
import play.routing.HandlerDef;
import play.routing.Router;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class RecordCookieSizeFilter extends Filter {
  private static final Logger logger = LoggerFactory.getLogger(RecordCookieSizeFilter.class);

  private Histogram PLAY_SESSION_COOKIE_SIZE =
    Histogram.build()
      .name("play_session_cookie_size_bytes")
      .linearBuckets(0, 4096, 512)
      .labelNames("action_method")
      .help("Size of the PLAY_SESSION cookie in bytes")
      .register();

  public RecordCookieSizeFilter(Materializer mat) {
    super(mat);
  }

  @Override
  public CompletionStage<Result> apply(Function<Http.RequestHeader, CompletionStage<Result>> next, Http.RequestHeader requestHeader) {
    HandlerDef handlerDef = requestHeader.attrs().get(Router.Attrs.HANDLER_DEF);
    String actionMethod = handlerDef.controller() + "." + handlerDef.method();
    requestHeader.getCookie("PLAY_SESSION").ifPresent(cookie -> {
      PLAY_SESSION_COOKIE_SIZE.labels(actionMethod).observe(cookie.value().length());
    });
    return next.apply(requestHeader);
  }
}
