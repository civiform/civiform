package controllers.monitoring;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import controllers.CiviFormController;
import io.prometheus.client.CollectorRegistry;
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

  @Inject
  public MetricsController(CollectorRegistry collectorRegistry, Config config) {
    this.collectorRegistry = checkNotNull(collectorRegistry);
    this.metricsEnabled = checkNotNull(config).getBoolean("server_metrics.enabled");
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
      TextFormat.write004(writer, collectorRegistry.metricFamilySamples());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return ok(writer.toString()).as(TextFormat.CONTENT_TYPE_004);
  }
}
