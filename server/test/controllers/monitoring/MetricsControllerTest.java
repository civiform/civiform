package controllers.monitoring;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.contentAsString;
import static services.settings.SettingsService.CIVIFORM_SETTINGS_ATTRIBUTE_KEY;
import static support.FakeRequestBuilder.fakeRequest;

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
import play.libs.typedmap.TypedMap;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.monitoring.EmailSendMetrics;
import services.monitoring.MonitoringMetricCounters;
import services.program.ProgramDefinition;
import services.settings.SettingsManifest;
import support.ProgramBuilder;

public class MetricsControllerTest extends WithMockedProfiles {
  private static final String FEATURE_FLAG = "ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS";
  private static final String EXPERIMENTAL_FLAG = "API_BRIDGE_ENABLED";

  @Before
  public void setUp() {
    resetDatabase();
  }

  @Test
  public void getMetrics_returnsMetricData() {
    MetricsController controllerWithMetricsEnabled = createController(/* metricsEnabled= */ true);

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

    Result metricsResult = controllerWithMetricsEnabled.getMetrics(fakeRequest());

    String metricsContent = contentAsString(controllerWithMetricsEnabled.getMetrics(fakeRequest()));

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
  public void getMetrics_includesFeatureFlagsAndBuildInfo() {
    MetricsController controller = createController(/* metricsEnabled= */ true);

    String metricsContent = contentAsString(controller.getMetrics(fakeRequest()));

    assertThat(getFeatureFlagValue(metricsContent, FEATURE_FLAG)).isNotNull();
    // Flags in the "Experimental" section are reported alongside the "Feature Flags" section.
    assertThat(getFeatureFlagValue(metricsContent, EXPERIMENTAL_FLAG)).isNotNull();
    assertThat(metricsContent).contains("civiform_build_info{image_tag=\"");
  }

  @Test
  public void getMetrics_featureFlagGaugeUsesAdminWriteableSettings() {
    MetricsController controller = createController(/* metricsEnabled= */ true);

    String enabledContent =
        contentAsString(controller.getMetrics(requestWithSetting(FEATURE_FLAG, "true")));
    assertThat(getFeatureFlagValue(enabledContent, FEATURE_FLAG)).isEqualTo("1.0");

    String disabledContent =
        contentAsString(controller.getMetrics(requestWithSetting(FEATURE_FLAG, "false")));
    assertThat(getFeatureFlagValue(disabledContent, FEATURE_FLAG)).isEqualTo("0.0");
  }

  @Test
  public void getMetrics_latencyMetricsUseCorrectPrometheusTypes() {
    String metricsContent =
        contentAsString(createController(/* metricsEnabled= */ true).getMetrics(fakeRequest()));

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

    String metricsContent =
        contentAsString(createController(/* metricsEnabled= */ true).getMetrics(fakeRequest()));
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
    MetricsController controllerWithoutMetricsEnabled =
        createController(/* metricsEnabled= */ false);
    assertThat(controllerWithoutMetricsEnabled.getMetrics(fakeRequest()).status()).isEqualTo(404);
  }

  @Test
  public void getMetrics_invalidFlagValueDoesNotFailScrape() {
    // With no stored settings on the request, the flag is read from config, where it isn't a
    // boolean.
    SettingsManifest settingsManifest =
        new SettingsManifest(
            ConfigFactory.parseMap(ImmutableMap.of(FEATURE_FLAG.toLowerCase(Locale.ROOT), "1")));
    MetricsController controller = createController(/* metricsEnabled= */ true, settingsManifest);

    Result result = controller.getMetrics(fakeRequest());

    assertThat(result.status()).isEqualTo(200);
    assertThat(contentAsString(result)).contains("civiform_build_info{image_tag=\"");
  }

  private MetricsController createController(boolean metricsEnabled) {
    return createController(metricsEnabled, instanceOf(SettingsManifest.class));
  }

  private MetricsController createController(
      boolean metricsEnabled, SettingsManifest settingsManifest) {
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.<String, String>builder()
                .put("civiform_server_metrics_enabled", String.valueOf(metricsEnabled))
                .build());
    return new MetricsController(
        config,
        instanceOf(ProfileUtils.class),
        instanceOf(VersionRepository.class),
        instanceOf(MonitoringMetricCounters.class),
        settingsManifest);
  }

  private static Http.Request requestWithSetting(String settingName, String value) {
    return fakeRequest()
        .withAttrs(
            TypedMap.empty()
                .put(CIVIFORM_SETTINGS_ATTRIBUTE_KEY, ImmutableMap.of(settingName, value)));
  }

  /**
   * Returns the value of the flag's series, or null if it's missing. The current Prometheus client
   * writes a trailing comma after the last label, for example {@code
   * civiform_feature_flag_enabled{flag="X",} 1.0}, so the comma is optional here.
   */
  private static String getFeatureFlagValue(String metricsContent, String flagName) {
    Matcher matcher =
        Pattern.compile(
                "civiform_feature_flag_enabled\\{flag=\""
                    + Pattern.quote(flagName)
                    + "\",?\\} (\\S+)")
            .matcher(metricsContent);
    return matcher.find() ? matcher.group(1) : null;
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
   * {@code # TYPE} line. Counter samples carry {@code _total}; simpleclient's text format keeps it
   * on the {@code # TYPE} line but OpenMetrics and the 1.x client drop it, so it is stripped too. A
   * labeled family emits no sample lines at all until first observed, so matching on the family
   * rather than the sample name is what makes this check independent of whether anything has been
   * observed yet.
   */
  private static ImmutableSet<String> familyCandidates(String name) {
    ImmutableSet.Builder<String> candidates = ImmutableSet.builder();
    candidates.add(name);
    for (String suffix : ImmutableList.of("_bucket", "_count", "_sum", "_total")) {
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
   * Extracts metric names from a PromQL expression by stripping label selectors, label lists, range
   * selectors, and Grafana variables, then keeping every identifier that is not a PromQL function
   * or keyword.
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
