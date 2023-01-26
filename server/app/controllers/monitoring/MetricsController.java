package controllers.monitoring;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import controllers.CiviFormController;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
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

    var counts = new HashMap<String,  Double>() ;
    var totalTimes = new HashMap<String,  Double>() ;
    Enumeration<Collector.MetricFamilySamples> enumer = collectorRegistry.metricFamilySamples();
    while (enumer.hasMoreElements()) {
      var samples = enumer.nextElement();
      for (var sample: samples.samples) {
        var name = sample.name;
        var labels = new HashMap<String, String>();
        for(int i = 0; i < sample.labelNames.size(); ++i) {
          labels.put(sample.labelNames.get(i), sample.labelValues.get(i));
        }
        System.out.println(name);

        if (name.endsWith("_sum")) {
          double value = sample.value;
          var subName = name.substring(0, name.length() - 4);
          var measure = String.format("%s_%s_%s_%s_%s", subName, labels.get("RouteActionMethod"), labels.get("Path"), labels.get("Status"), labels.get("Verb"));
          totalTimes.put(measure, value);
        }
        if (name.endsWith("_count")) {
          double value = sample.value;
          var subName = name.substring(0, name.length() - 6);
          var measure = String.format("%s_%s_%s_%s_%s", subName, labels.get("RouteActionMethod"), labels.get("Path"), labels.get("Status"), labels.get("Verb"));
          counts.put(measure, value);
        }
      }
    }
    var metricByTime = new HashMap<Double, String>();
    counts.keySet().stream().sorted().forEach(
      key -> {
        double count = counts.get(key);
        double totalTime = totalTimes.get(key);
        var metric = String.format("%s: num %f avg ms: %f\n", key, count, totalTime/count);
        metricByTime.put(totalTime/count, metric);
      }
    );
    metricByTime.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).map(Map.Entry::getValue).forEach(writer::write);


    return ok(writer.toString()).as(TextFormat.CONTENT_TYPE_004);
  }
}
