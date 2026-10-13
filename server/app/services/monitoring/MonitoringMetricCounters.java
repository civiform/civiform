package services.monitoring;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class MonitoringMetricCounters {
  private final Counter queryMetricCount;
  // Mean and max are point-in-time observations of the most recent collection interval, not
  // additive quantities, so they are Gauges. Summing them into a Counter produces a
  // monotonically-increasing series that is neither a mean nor a max.
  private final Gauge queryMetricMeanLatency;
  private final Gauge queryMetricMaxLatency;
  private final Counter queryMetricTotalLatency;
  private final Counter urlWithProgramIdCall;

  @Inject
  public MonitoringMetricCounters() {
    queryMetricCount =
        Counter.build()
            .name("ebean_queries_total")
            .help("Count of database queries")
            .labelNames("name", "location", "className")
            .register();

    queryMetricMeanLatency =
        Gauge.build()
            .name("ebean_queries_mean_latency_micros")
            .help("Mean latency of database queries in micros")
            .labelNames("name", "location", "className")
            .register();

    queryMetricMaxLatency =
        Gauge.build()
            .name("ebean_queries_max_latency_micros")
            .help("Max latency of database queries in micros")
            .labelNames("name", "location", "className")
            .register();

    queryMetricTotalLatency =
        Counter.build()
            .name("ebean_queries_total_latency_micros")
            .help("Total latency of database queries in micros")
            .labelNames("name", "location", "className")
            .register();

    urlWithProgramIdCall =
        Counter.build()
            .name("url_with_program_id_call_total")
            .help("Count of calls to program-related URLs")
            .labelNames("route", "programId")
            .register();
  }

  public Counter getQueryMetricCount() {
    return queryMetricCount;
  }

  public Gauge getQueryMetricMeanLatency() {
    return queryMetricMeanLatency;
  }

  public Gauge getQueryMetricMaxLatency() {
    return queryMetricMaxLatency;
  }

  public Counter getQueryMetricTotalLatency() {
    return queryMetricTotalLatency;
  }

  public Counter getUrlWithProgramIdCall() {
    return urlWithProgramIdCall;
  }
}
