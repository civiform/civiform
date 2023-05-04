package controllers.monitoring;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import controllers.CiviFormController;
import io.ebean.DB;
import io.ebean.Database;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import javax.inject.Inject;
import play.mvc.Result;

/**
 * Controller for exporting Prometheus server metrics via HTTP. Based on the implementation found in
 * {@link com.github.stijndehaes.playprometheusfilters.controllers.PrometheusController} and
 * customized to allow disabling via configuration flag.
 */
public final class MetricsController extends CiviFormController {

  private final boolean metricsEnabled;
  private final CollectorRegistry collectorRegistry;
  private final Database database;

  private static final Counter queryMetricCount =
      Counter.build()
          .name("ebean_query_metric_count")
          .help("Count of database queries")
          .labelNames("name")
          .register();

  private static final Counter queryMetricMeanLatency =
      Counter.build()
          .name("ebean_query_metric_mean_latency")
          .help("Mean latency of database queries")
          .labelNames("name")
          .register();

  private static final Counter queryMetricMaxLatency =
      Counter.build()
          .name("ebean_query_metric_max_latency")
          .help("Max latency of database queries")
          .labelNames("name")
          .register();

  private static final Counter queryMetricTotalLatency =
      Counter.build()
          .name("ebean_query_metric_total_latency")
          .help("Total latency of database queries")
          .labelNames("name")
          .register();

  // The start index we use for the metric substring. By default, the metric names start with
  // "orm.", which is why we use 4 as the start index.
  private static final int SUBSTRING_INDEX = 4;

  @Inject
  public MetricsController(CollectorRegistry collectorRegistry, Config config) {
    this.collectorRegistry = checkNotNull(collectorRegistry);
    this.metricsEnabled = checkNotNull(config).getBoolean("server_metrics.enabled");
    this.database = DB.getDefault();
  }

  /**
   * Exports server metrics in Prometheus 0.0.4 text format
   * (https://github.com/Showmax/prometheus-docs/blob/master/content/docs/instrumenting/exposition_formats.md#format-version-004).
   */
  public Result getMetrics() {
    if (!metricsEnabled) {
      return notFound();
    }

    var writer = new StringWriter();

    try {
      database
          .getMetaInfoManager()
          .collectMetrics()
          .getQueryMetrics()
          .forEach(
              metric -> {
                String name = metric.getName().substring(SUBSTRING_INDEX);
                queryMetricCount.labels(name).inc((double) metric.getCount());
                queryMetricMeanLatency.labels(name).inc((double) metric.getMean());
                queryMetricMaxLatency.labels(name).inc((double) metric.getMax());
                queryMetricTotalLatency.labels(name).inc((double) metric.getTotal());
              });
      TextFormat.write004(writer, collectorRegistry.metricFamilySamples());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return ok(writer.toString()).as(TextFormat.CONTENT_TYPE_004);
  }
}
