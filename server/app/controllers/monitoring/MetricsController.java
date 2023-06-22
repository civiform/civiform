package controllers.monitoring;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import com.google.common.annotations.VisibleForTesting;
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
import repository.VersionRepository;

/**
 * Controller for exporting Prometheus server metrics via HTTP. Based on the implementation found in
 * {@link com.github.stijndehaes.playprometheusfilters.controllers.PrometheusController} and
 * customized to allow disabling via configuration flag.
 */
public final class MetricsController extends CiviFormController {

  private final boolean metricsEnabled;
  private final CollectorRegistry collectorRegistry;
  private final Database database;
  private static Counter QUERY_METRIC_COUNT;
  private static Counter QUERY_METRIC_MEAN_LATENCY;
  private static Counter QUERY_METRIC_MAX_LATENCY;
  private static Counter QUERY_METRIC_TOTAL_LATENCY;

  static {
    initializeCounters();
  }

  // The start index we use for the metric substring. By default, the metric names start with
  // "orm.", which is why we use 4 as the start index.
  private static final int SUBSTRING_INDEX = 4;

  @Inject
  public MetricsController(
      Config config, ProfileUtils profileUtils, VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.collectorRegistry = checkNotNull(CollectorRegistry.defaultRegistry);
    this.metricsEnabled = checkNotNull(config).getBoolean("civiform_server_metrics_enabled");
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
                QUERY_METRIC_COUNT.labels(name).inc((double) metric.getCount());
                QUERY_METRIC_MEAN_LATENCY.labels(name).inc((double) metric.getMean());
                QUERY_METRIC_MAX_LATENCY.labels(name).inc((double) metric.getMax());
                QUERY_METRIC_TOTAL_LATENCY.labels(name).inc((double) metric.getTotal());
              });
      TextFormat.write004(writer, collectorRegistry.metricFamilySamples());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return ok(writer.toString()).as(TextFormat.CONTENT_TYPE_004);
  }

  @VisibleForTesting
  static void initializeCounters() {
    QUERY_METRIC_COUNT =
        Counter.build()
            .name("ebean_queries_total")
            .help("Count of database queries")
            .labelNames("name")
            .register();

    QUERY_METRIC_MEAN_LATENCY =
        Counter.build()
            .name("ebean_queries_mean_latency_micros")
            .help("Mean latency of database queries in micros")
            .labelNames("name")
            .register();

    QUERY_METRIC_MAX_LATENCY =
        Counter.build()
            .name("ebean_queries_max_latency_micros")
            .help("Max latency of database queries in micros")
            .labelNames("name")
            .register();

    QUERY_METRIC_TOTAL_LATENCY =
        Counter.build()
            .name("ebean_queries_total_latency_micros")
            .help("Total latency of database queries in micros")
            .labelNames("name")
            .register();
  }
}
