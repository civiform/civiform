package filters;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.createHandlerDef;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.typesafe.config.ConfigFactory;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.CollectorRegistry;
import java.util.Collections;
import java.util.List;
import org.apache.pekko.stream.testkit.NoMaterializer$;
import org.junit.Before;
import org.junit.Test;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import play.routing.Router;

public class PrometheusRequestMetricsFilterTest {
  private PrometheusRequestMetricsFilter filter;

  @Before
  public void setUp() {
    CollectorRegistry.defaultRegistry.clear();
    filter =
        new PrometheusRequestMetricsFilter(
            CollectorRegistry.defaultRegistry,
            ConfigFactory.parseMap(
                ImmutableMap.of(
                    PrometheusRequestMetricsFilter.EXCLUDE_PATHS_KEY,
                    ImmutableList.of("/metrics"))),
            MoreExecutors.directExecutor());
  }

  @Test
  public void apply_registersLatencyHistogramAndRequestCounter() {
    // The client strips the "_total" suffix from a counter's family name; the sample keeps it.
    assertThat(familyNames())
        .containsExactlyInAnyOrder("requests_latency_seconds", "http_requests");
  }

  @Test
  public void apply_recordsRouteAndStatusLabelsForMatchedRoute() throws Exception {
    Http.Request request =
        fakeRequestBuilder()
            .location("GET", "/programs/1")
            .attr(
                Router.Attrs.HANDLER_DEF,
                createHandlerDef(getClass().getClassLoader(), "/programs/$id<[^/]+>"))
            .build();

    Result result = run(request, Results.notFound("missing"));

    assertThat(result.status()).isEqualTo(404);
    Sample count = sample("http_requests", "http_requests_total");
    assertThat(count.value).isEqualTo(1);
    assertThat(count.labelNames).containsExactly("method", "status", "controller", "path", "verb");
    assertThat(count.labelValues)
        .containsExactly("index", "404", "controllers.MyFakeController", "/programs/$id", "GET");

    Sample latencyCount = sample("requests_latency_seconds", "requests_latency_seconds_count");
    assertThat(latencyCount.value).isEqualTo(1);
    assertThat(latencyCount.labelNames)
        .containsExactly("RouteActionMethod", "Status", "Controller", "Path", "Verb");
    assertThat(latencyCount.labelValues)
        .containsExactly("index", "404", "controllers.MyFakeController", "/programs/$id", "GET");
    Sample latencySum = sample("requests_latency_seconds", "requests_latency_seconds_sum");
    assertThat(latencySum.value).isGreaterThan(0);
  }

  @Test
  public void apply_usesUnmatchedLabelsWhenNoRouteMatched() throws Exception {
    Http.Request request = fakeRequestBuilder().location("GET", "/nowhere").build();

    run(request, Results.ok("OK"));

    Sample count = sample("http_requests", "http_requests_total");
    assertThat(count.labelValues)
        .containsExactly(
            PrometheusRequestMetricsFilter.UNMATCHED_ROUTE,
            "200",
            PrometheusRequestMetricsFilter.UNMATCHED_CONTROLLER,
            PrometheusRequestMetricsFilter.UNMATCHED_PATH,
            PrometheusRequestMetricsFilter.UNMATCHED_VERB);
  }

  @Test
  public void apply_skipsExcludedPaths() throws Exception {
    Http.Request request = fakeRequestBuilder().location("GET", "/metrics").build();

    Result result = run(request, Results.ok("OK"));

    assertThat(result.status()).isEqualTo(200);
    assertThat(samples("http_requests")).isEmpty();
    assertThat(samples("requests_latency_seconds")).isEmpty();
  }

  private Result run(Http.Request request, Result response) throws Exception {
    return filter
        .apply(EssentialAction.of(r -> Accumulator.done(response)))
        .apply(request)
        .run(NoMaterializer$.MODULE$)
        .toCompletableFuture()
        .get();
  }

  private static List<MetricFamilySamples> families() {
    return Collections.list(CollectorRegistry.defaultRegistry.metricFamilySamples());
  }

  private static List<String> familyNames() {
    return families().stream().map(family -> family.name).toList();
  }

  private static List<Sample> samples(String familyName) {
    return families().stream()
        .filter(family -> family.name.equals(familyName))
        .flatMap(family -> family.samples.stream())
        .toList();
  }

  private static Sample sample(String familyName, String sampleName) {
    return samples(familyName).stream()
        .filter(sample -> sample.name.equals(sampleName))
        .findFirst()
        .orElseThrow();
  }
}
