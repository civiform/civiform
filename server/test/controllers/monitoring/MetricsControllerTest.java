package controllers.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.contentAsString;
import static services.settings.SettingsService.CIVIFORM_SETTINGS_ATTRIBUTE_KEY;
import static support.FakeRequestBuilder.fakeRequest;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import controllers.WithMockedProfiles;
import java.util.Locale;
import models.ApplicantModel;
import models.ApplicationModel;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import play.libs.typedmap.TypedMap;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
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

    assertThat(metricsContent).contains(getFeatureFlagSeriesName(FEATURE_FLAG));
    // Flags in the "Experimental" section are reported alongside the "Feature Flags" section.
    assertThat(metricsContent).contains(getFeatureFlagSeriesName(EXPERIMENTAL_FLAG));
    assertThat(metricsContent).contains("civiform_build_info{image_tag=\"");
  }

  @Test
  public void getMetrics_featureFlagGaugeUsesAdminWriteableSettings() {
    MetricsController controller = createController(/* metricsEnabled= */ true);

    String enabledContent =
        contentAsString(controller.getMetrics(requestWithSetting(FEATURE_FLAG, "true")));
    assertThat(enabledContent).contains(getFeatureFlagSeriesName(FEATURE_FLAG) + " 1.0");

    String disabledContent =
        contentAsString(controller.getMetrics(requestWithSetting(FEATURE_FLAG, "false")));
    assertThat(disabledContent).contains(getFeatureFlagSeriesName(FEATURE_FLAG) + " 0.0");
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
  public void getMetrics_returns404WhenMetricsNotEnabled() {
    MetricsController controllerWithoutMetricsEnabled =
        createController(/* metricsEnabled= */ false);
    assertThat(controllerWithoutMetricsEnabled.getMetrics(fakeRequest()).status()).isEqualTo(404);
  }

  private MetricsController createController(boolean metricsEnabled) {
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
        instanceOf(SettingsManifest.class));
  }

  private static Http.Request requestWithSetting(String settingName, String value) {
    return fakeRequest()
        .withAttrs(
            TypedMap.empty()
                .put(CIVIFORM_SETTINGS_ATTRIBUTE_KEY, ImmutableMap.of(settingName, value)));
  }

  /**
   * The Prometheus text format writes a trailing comma after the last label, for example {@code
   * civiform_feature_flag_enabled{flag="X",} 1.0}.
   */
  private static String getFeatureFlagSeriesName(String flagName) {
    return String.format("civiform_feature_flag_enabled{flag=\"%s\",}", flagName);
  }

  private String getEbeanCountName(String queryName) {
    return String.format("ebean_queries_total{name=\"%s", queryName);
  }
}
