package controllers.monitoring;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import com.typesafe.config.Config;
import controllers.CiviFormController;
import io.ebean.DB;
import io.ebean.Database;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.StringWriter;
import java.util.ConcurrentModificationException;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Result;
import repository.VersionRepository;
import services.monitoring.MonitoringMetricCounters;

/**
 * Controller for exporting Prometheus server metrics via HTTP. Based on the implementation found in
 * {@link com.github.stijndehaes.playprometheusfilters.controllers.PrometheusController} and
 * customized to allow disabling via configuration flag.
 */
public final class MetricsController extends CiviFormController {
  private static final Logger LOGGER = LoggerFactory.getLogger(MetricsController.class);

  private final boolean metricsEnabled;
  private final CollectorRegistry collectorRegistry;
  private final Database database;

  // The start index we use for the metric substring. By default, the metric names start with
  // "orm.", which is why we use 4 as the start index.
  private static final int NAME_SUBSTRING_INDEX = 4;

  // The start index we use for the metric substring. By default, the metric names start with
  // "class ", which is why we use 6 as the start index.
  private static final int CLASS_SUBSTRING_INDEX = 6;
  private final MonitoringMetricCounters monitoringMetricCounters;

  @Inject
  public MetricsController(
      Config config,
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      MonitoringMetricCounters monitoringMetricCounters) {
    super(profileUtils, versionRepository);
    this.monitoringMetricCounters = monitoringMetricCounters;
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

    try {
      database
          .metaInfo()
          .collectMetrics()
          .queryMetrics()
          .forEach(
              metric -> {
                String name = metric.name().substring(NAME_SUBSTRING_INDEX);
                String className = metric.type().toString().substring(CLASS_SUBSTRING_INDEX);
                String location = metric.location() != null ? metric.location() : "";
                // When we use JPA in the model to get the data, we often see incorrect information
                // after the underscore. In these cases, we set the model class for the name and
                // location.
                // TODO(#5934) remove reliance on JPA for database queries
                if (name.contains("_")) {
                  name = className;
                  location = className;
                }
                monitoringMetricCounters
                    .getQueryMetricCount()
                    .labels(name, location, className)
                    .inc((double) metric.count());
                monitoringMetricCounters
                    .getQueryMetricMeanLatency()
                    .labels(name, location, className)
                    .inc((double) metric.mean());
                monitoringMetricCounters
                    .getQueryMetricMaxLatency()
                    .labels(name, location, className)
                    .inc((double) metric.max());
                monitoringMetricCounters
                    .getQueryMetricTotalLatency()
                    .labels(name, location, className)
                    .inc((double) metric.total());
              });

      var writer = new StringWriter();
      TextFormat.write004(writer, collectorRegistry.metricFamilySamples());
      return ok(writer.toString()).as(TextFormat.CONTENT_TYPE_004);
    } catch (ConcurrentModificationException e) {
      // This exception can often be triggered if you start running the full browser test
      // suite and then spam calling `/metrics`.
      //
      // Basically this state can happen when calling `database.metaInfo().collectMetrics()`
      // method and the metrics are either being updated or a lock can't be immediately gotten.
      //
      // Avoid using synchronize to force a lock on the database object as it may adversely
      // affecting performance.
      //
      // Since the `/metrics` endpoint is called very frequently any data from a failed call
      // gets rolled into the next successful call to `/metrics`.
      var errorMsg =
          """
          Attempted to collect database metrics for Prometheus, but could not get a lock at this
          time. This is safe to ignore. The metrics will be included on the next successful call.
          """;

      LOGGER.info(errorMsg.stripIndent(), e);
    } catch (Exception e) {
      LOGGER.error(e.getMessage());
    }

    return internalServerError();
  }
}
