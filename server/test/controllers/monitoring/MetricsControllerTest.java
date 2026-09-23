package controllers.monitoring;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.contentAsString;

import auth.ProfileUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import controllers.WithMockedProfiles;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import models.ApplicantModel;
import models.ApplicationModel;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import repository.VersionRepository;
import services.monitoring.EmailSendMetrics;
import services.monitoring.MonitoringMetricCounters;
import services.program.ProgramDefinition;
import support.ProgramBuilder;

public class MetricsControllerTest extends WithMockedProfiles {
  @Before
  public void setUp() {
    resetDatabase();
  }

  @Test
  public void getMetrics_returnsMetricData() {
    MetricsController controllerWithMetricsEnabled = controllerWithMetricsEnabled();

    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    VersionRepository versionRepository = instanceOf(VersionRepository.class);
    ApplicantModel applicant = createApplicantWithMockedProfile();
    applicant.getApplicantData().setPreferredLocale(Locale.ENGLISH);
    applicant.save();
    ApplicationModel app =
        new ApplicationModel(applicant, programDefinition.toProgram(), LifecycleStage.DRAFT);
    app.save();
    resourceCreator().insertDraftProgram(programDefinition.adminName());
    versionRepository.publishNewSynchronizedVersion();

    Result metricsResult = controllerWithMetricsEnabled.getMetrics();

    String metricsContent = contentAsString(controllerWithMetricsEnabled.getMetrics());

    assertThat(metricsResult.status()).isEqualTo(200);
    assertThat(metricsContent).contains("ebean_queries_mean_latency_micros");
    assertThat(metricsContent).contains("ebean_queries_max_latency_micros");
    assertThat(metricsContent).contains("ebean_queries_total_latency_micros");
    assertThat(metricsContent).contains(getEbeanCountName("models.ProgramModel"));
    assertThat(metricsContent).contains(getEbeanCountName("models.Question"));
    assertThat(metricsContent).contains(getEbeanCountName("VersionModel.byId"));
    assertThat(metricsContent).contains("location=\"repository.VersionRepository.getActiveVersion");
    assertThat(metricsContent).contains("className=\"models.VersionModel");
  }

  @Test
  public void getMetrics_latencyMetricsUseCorrectPrometheusTypes() {
    String metricsContent = contentAsString(controllerWithMetricsEnabled().getMetrics());

    // Mean and max are point-in-time observations of the most recent collection interval and
    // are not additive, so they must be gauges. Declaring them as counters makes each series a
    // running sum that is neither a mean nor a max.
    assertThat(metricsContent).contains("# TYPE ebean_queries_mean_latency_micros gauge");
    assertThat(metricsContent).contains("# TYPE ebean_queries_max_latency_micros gauge");
    // Count and total latency are additive and must stay counters. Note the client appends
    // "_total" to a counter's name unless it already ends in "_total", which is why
    // ebean_queries_total keeps its name but total latency gains a second suffix.
    assertThat(metricsContent).contains("# TYPE ebean_queries_total counter");
    assertThat(metricsContent).contains("# TYPE ebean_queries_total_latency_micros_total counter");
  }

  @Test
  public void getMetrics_everyDashboardPanelMetricIsRegistered() throws IOException {
    // Metrics are registered when their holder is constructed. The test application binds a fake
    // email client that never constructs EmailSendMetrics, so construct it here; otherwise the
    // scrape would lack families that production registers, and the check would be about the
    // test wiring rather than the dashboard.
    instanceOf(EmailSendMetrics.class);

    String metricsContent = contentAsString(controllerWithMetricsEnabled().getMetrics());
    ImmutableSet<String> families = registeredMetricFamilies(metricsContent);
    ImmutableSet<String> referenced = dashboardMetricNames();

    // Guard against either side silently matching nothing.
    assertThat(families).isNotEmpty();
    assertThat(referenced).isNotEmpty();

    ImmutableList<String> unregistered =
        referenced.stream()
            .filter(name -> familyCandidates(name).stream().noneMatch(families::contains))
            .collect(toImmutableList());
    assertThat(unregistered)
        .describedAs(
            "metric names referenced by monitoring/grafana/dashboards/civiform-dashboard.json"
                + " whose family is never registered; a panel querying one of these renders"
                + " empty. Registered families: %s",
            families)
        .isEmpty();
  }

  @Test
  public void getMetrics_returns404WhenMetricsNotEnabled() {
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.<String, String>builder()
                .put("civiform_server_metrics_enabled", "false")
                .build());
    MetricsController controllerWithoutMetricsEnabled =
        new MetricsController(
            config,
            instanceOf(ProfileUtils.class),
            instanceOf(VersionRepository.class),
            instanceOf(MonitoringMetricCounters.class));
    assertThat(controllerWithoutMetricsEnabled.getMetrics().status()).isEqualTo(404);
  }

  private MetricsController controllerWithMetricsEnabled() {
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.<String, String>builder()
                .put("civiform_server_metrics_enabled", "true")
                .build());
    return new MetricsController(
        config,
        instanceOf(ProfileUtils.class),
        instanceOf(VersionRepository.class),
        instanceOf(MonitoringMetricCounters.class));
  }

  private static final Pattern TYPE_LINE =
      Pattern.compile("^# TYPE (\\S+) \\S+$", Pattern.MULTILINE);

  /** Metric family names, taken from the {@code # TYPE} lines of a Prometheus text exposition. */
  private static ImmutableSet<String> registeredMetricFamilies(String exposition) {
    ImmutableSet.Builder<String> families = ImmutableSet.builder();
    Matcher matcher = TYPE_LINE.matcher(exposition);
    while (matcher.find()) {
      families.add(matcher.group(1));
    }
    return families.build();
  }

  /**
   * The family names a dashboard metric name could belong to. Histogram and summary samples carry
   * {@code _bucket}, {@code _count} and {@code _sum} suffixes that do not appear on the family's
   * {@code # TYPE} line, and a labeled family emits no sample lines at all until first observed,
   * so matching on the family rather than the sample name is what makes this check independent
   * of whether anything has been observed yet.
   */
  private static ImmutableSet<String> familyCandidates(String name) {
    ImmutableSet.Builder<String> candidates = ImmutableSet.builder();
    candidates.add(name);
    for (String suffix : ImmutableList.of("_bucket", "_count", "_sum")) {
      if (name.endsWith(suffix)) {
        candidates.add(name.substring(0, name.length() - suffix.length()));
      }
    }
    return candidates.build();
  }

  private static final File DASHBOARD =
      new File("../monitoring/grafana/dashboards/civiform-dashboard.json");

  private static final Pattern IDENTIFIER = Pattern.compile("[a-zA-Z_:][a-zA-Z0-9_:]*");

  // PromQL functions, aggregators, and keywords that appear in panel expressions but are not
  // metric names.
  private static final ImmutableSet<String> PROMQL_WORDS =
      ImmutableSet.of(
          "rate",
          "irate",
          "increase",
          "sum",
          "avg",
          "min",
          "max",
          "count",
          "histogram_quantile",
          "avg_over_time",
          "max_over_time",
          "min_over_time",
          "sum_over_time",
          "by",
          "without",
          "on",
          "ignoring",
          "group_left",
          "group_right",
          "and",
          "or",
          "unless",
          "offset",
          "bool");

  /** Every metric name referenced in any panel expression of the dev Grafana dashboard. */
  private static ImmutableSet<String> dashboardMetricNames() throws IOException {
    JsonNode root = new ObjectMapper().readTree(DASHBOARD);
    ImmutableSet.Builder<String> names = ImmutableSet.builder();
    collectMetricNames(root.path("panels"), names);
    return names.build();
  }

  private static void collectMetricNames(JsonNode panels, ImmutableSet.Builder<String> names) {
    for (JsonNode panel : panels) {
      for (JsonNode target : panel.path("targets")) {
        if (target.hasNonNull("expr")) {
          names.addAll(metricNamesIn(target.get("expr").asText()));
        }
      }
      // Row panels nest their children under "panels".
      collectMetricNames(panel.path("panels"), names);
    }
  }

  /**
   * Extracts metric names from a PromQL expression by stripping label selectors, label lists,
   * range selectors, and Grafana variables, then keeping every identifier that is not a PromQL
   * function or keyword.
   */
  private static ImmutableSet<String> metricNamesIn(String expr) {
    String stripped =
        expr.replaceAll("\\{[^}]*\\}", "") // {label="value"} selectors
            .replaceAll("(?i)\\b(by|without|on|ignoring)\\s*\\([^)]*\\)", "") // label lists
            .replaceAll("\\$\\w+", "") // Grafana variables such as $__interval
            .replaceAll("\\[[^\\]]*\\]", ""); // [5m] range selectors
    ImmutableSet.Builder<String> names = ImmutableSet.builder();
    Matcher matcher = IDENTIFIER.matcher(stripped);
    while (matcher.find()) {
      String identifier = matcher.group();
      if (!PROMQL_WORDS.contains(identifier)) {
        names.add(identifier);
      }
    }
    return names.build();
  }

  private String getEbeanCountName(String queryName) {
    return String.format("ebean_queries_total{name=\"%s", queryName);
  }
}
