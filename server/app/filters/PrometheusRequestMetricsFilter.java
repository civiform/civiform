package filters;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import play.api.routing.HandlerDef;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Http;
import play.mvc.Result;
import play.routing.Router;

/**
 * Filter that records the latency of every request in a histogram and counts requests in a counter,
 * both labeled by the matched route and the response status.
 *
 * <p>Metric and label names match what the Grafana dashboards query.
 *
 * <p>Requests whose URI matches one of the regexes in {@code prometheus.exclude_paths} are not
 * recorded. By default that is the {@code /metrics} scrape endpoint itself.
 */
@Singleton
public final class PrometheusRequestMetricsFilter extends EssentialFilter {
  static final String EXCLUDE_PATHS_KEY = "prometheus.exclude_paths";

  static final String UNMATCHED_ROUTE = "unmatchedRoute";
  static final String UNMATCHED_CONTROLLER = "unmatchedController";
  static final String UNMATCHED_PATH = "unmatchedPath";
  static final String UNMATCHED_VERB = "unmatchedVerb";

  // Dynamic route segments render as "$id<[^/]+>" in HandlerDef#path. Stripping the regex leaves
  // the readable "$id".
  private static final Pattern DYNAMIC_PATH_PART = Pattern.compile("<\\[\\^/]\\+>");

  private final Histogram requestLatency;
  private final Counter requestCount;
  private final ImmutableList<Pattern> excludePaths;
  private final Executor exec;

  @Inject
  public PrometheusRequestMetricsFilter(CollectorRegistry registry, Config config, Executor exec) {
    checkNotNull(registry);
    this.exec = checkNotNull(exec);

    this.requestLatency =
        Histogram.build()
            .name("requests_latency_seconds")
            .help("Request latency in seconds.")
            .labelNames("RouteActionMethod", "Status", "Controller", "Path", "Verb")
            .register(registry);
    this.requestCount =
        Counter.build()
            .name("http_requests_total")
            .help("Total amount of requests")
            .labelNames("method", "status", "controller", "path", "verb")
            .register(registry);
    this.excludePaths =
        config.getStringList(EXCLUDE_PATHS_KEY).stream()
            .map(Pattern::compile)
            .collect(toImmutableList());
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request -> {
          long startNanos = System.nanoTime();

          return next.apply(request)
              .map(
                  result -> {
                    if (!isExcluded(request.uri())) {
                      double seconds =
                          (System.nanoTime() - startNanos) / Collector.NANOSECONDS_PER_SECOND;
                      record(request, result, seconds);
                    }
                    return result;
                  },
                  exec);
        });
  }

  private boolean isExcluded(String uri) {
    return excludePaths.stream().anyMatch(pattern -> pattern.matcher(uri).find());
  }

  private void record(Http.RequestHeader request, Result result, double seconds) {
    Optional<HandlerDef> handlerDef = request.attrs().getOptional(Router.Attrs.HANDLER_DEF);
    String route = handlerDef.map(HandlerDef::method).orElse(UNMATCHED_ROUTE);
    String status = String.valueOf(result.status());
    String controller = handlerDef.map(HandlerDef::controller).orElse(UNMATCHED_CONTROLLER);
    String path =
        handlerDef
            .map(def -> DYNAMIC_PATH_PART.matcher(def.path()).replaceAll(""))
            .orElse(UNMATCHED_PATH);
    String verb = handlerDef.map(HandlerDef::verb).orElse(UNMATCHED_VERB);

    requestLatency.labels(route, status, controller, path, verb).observe(seconds);
    requestCount.labels(route, status, controller, path, verb).inc();
  }
}
