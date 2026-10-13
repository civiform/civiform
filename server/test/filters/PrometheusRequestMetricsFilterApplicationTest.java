package filters;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.route;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.CollectorRegistry;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import repository.ResetPostgres;

/**
 * Checks that {@link PrometheusRequestMetricsFilter} is wired into the application's filter chain
 * and records requests that go through the router.
 */
public class PrometheusRequestMetricsFilterApplicationTest extends ResetPostgres {

  @Test
  public void routedRequest_isCountedWithRouteLabels() {
    Http.RequestBuilder request =
        fakeRequestBuilder().location("GET", "/").header(Http.HeaderNames.HOST, "localhost");
    double before = requestCount("controllers.HomeController", "index");

    Result result = route(app, request);

    assertThat(result.status()).isLessThan(500);
    assertThat(requestCount("controllers.HomeController", "index")).isEqualTo(before + 1);
  }

  private static double requestCount(String controller, String method) {
    return Collections.list(CollectorRegistry.defaultRegistry.metricFamilySamples()).stream()
        .filter(family -> family.name.equals("http_requests"))
        .flatMap(family -> family.samples.stream())
        .filter(sample -> sample.name.equals("http_requests_total"))
        .filter(sample -> labelValue(sample, "controller").equals(controller))
        .filter(sample -> labelValue(sample, "method").equals(method))
        .mapToDouble(sample -> sample.value)
        .sum();
  }

  private static String labelValue(Sample sample, String labelName) {
    List<String> names = sample.labelNames;
    int index = names.indexOf(labelName);
    return index < 0 ? "" : sample.labelValues.get(index);
  }
}
