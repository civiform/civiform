package controllers.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.contentAsString;

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
import play.mvc.Result;
import repository.VersionRepository;
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

  private String getEbeanCountName(String queryName) {
    return String.format("ebean_queries_total{name=\"%s", queryName);
  }
}
