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
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.<String, String>builder()
                .put("civiform_server_metrics_enabled", "true")
                .build());
    MetricsController controllerWithMetricsEnabled =
        new MetricsController(
            config,
            instanceOf(ProfileUtils.class),
            instanceOf(VersionRepository.class),
            instanceOf(MonitoringMetricCounters.class));

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

  private String getEbeanCountName(String queryName) {
    return String.format("ebean_queries_total{name=\"%s", queryName);
  }
}
