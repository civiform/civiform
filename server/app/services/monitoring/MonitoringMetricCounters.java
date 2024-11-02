package services.monitoring;

import io.prometheus.client.Counter;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class MonitoringMetricCounters {
  private final Counter queryMetricCount;
  private final Counter queryMetricMeanLatency;
  private final Counter queryMetricMaxLatency;
  private final Counter queryMetricTotalLatency;

  @Inject
  public MonitoringMetricCounters() {
    queryMetricCount =
        Counter.build()
            .name("ebean_queries_total")
            .help("Count of database queries")
            .labelNames("name", "location", "className")
            .register();

    queryMetricMeanLatency =
        Counter.build()
            .name("ebean_queries_mean_latency_micros")
            .help("Mean latency of database queries in micros")
            .labelNames("name", "location", "className")
            .register();

    queryMetricMaxLatency =
        Counter.build()
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
  }

  public Counter getQueryMetricCount() {
    return queryMetricCount;
  }

  public Counter getQueryMetricMeanLatency() {
    return queryMetricMeanLatency;
  }

  public Counter getQueryMetricMaxLatency() {
    return queryMetricMaxLatency;
  }

  public Counter getQueryMetricTotalLatency() {
    return queryMetricTotalLatency;
  }
}
