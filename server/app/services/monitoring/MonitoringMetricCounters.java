package services.monitoring;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class MonitoringMetricCounters {
  private final Counter queryMetricCount;
  private final Counter queryMetricMeanLatency;
  private final Counter queryMetricMaxLatency;
  private final Counter queryMetricTotalLatency;
  private final Counter urlWithProgramIdCall;
  private final Gauge featureFlagEnabled;
  private final Gauge buildInfo;

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

    urlWithProgramIdCall =
        Counter.build()
            .name("url_with_program_id_call_total")
            .help("Count of calls to program-related URLs")
            .labelNames("route", "programId")
            .register();

    featureFlagEnabled =
        Gauge.build()
            .name("civiform_feature_flag_enabled")
            .help("Whether a CiviForm feature flag is enabled (1) or disabled (0)")
            .labelNames("flag")
            .register();

    buildInfo =
        Gauge.build()
            .name("civiform_build_info")
            .help("CiviForm build information. The value is always 1.")
            .labelNames("image_tag", "version")
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

  public Counter getUrlWithProgramIdCall() {
    return urlWithProgramIdCall;
  }

  public Gauge getFeatureFlagEnabled() {
    return featureFlagEnabled;
  }

  public Gauge getBuildInfo() {
    return buildInfo;
  }
}
