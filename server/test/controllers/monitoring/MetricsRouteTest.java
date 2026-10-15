package controllers.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.Helpers.testServerPort;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.route;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.BeforeClass;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import repository.ResetPostgres;
import services.settings.SettingsService;
import support.ProgramBuilder;
import support.ResourceCreator;

/**
 * Requests /metrics through the router and the filters. {@link MetricsControllerTest} calls the
 * controller directly, so it can't tell if {@code SettingsFilter} stops applying the stored
 * settings to this path.
 */
public class MetricsRouteTest extends ResetPostgres {
  private static final String FEATURE_FLAG = "ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS";
  private static final Pattern FEATURE_FLAG_SERIES =
      Pattern.compile(
          "civiform_feature_flag_enabled\\{flag=\"" + FEATURE_FLAG + "\",?\\} (\\S+)");

  // Metrics are off by default, so start the application with them turned on.
  @BeforeClass
  public static void startPlay() {
    app = fakeApplication(ImmutableMap.of("civiform_server_metrics_enabled", true));
    resourceCreator = new ResourceCreator(app.injector());
    Helpers.start(app);
    mat = app.asScala().materializer();
    ProgramBuilder.setInjector(app.injector());
  }

  @Test
  public void getMetrics_featureFlagGaugeFollowsStoredSettingThroughFilters() {
    SettingsService settingsService = instanceOf(SettingsService.class);
    ImmutableMap<String, String> stored =
        settingsService.loadSettings().toCompletableFuture().join().orElseThrow();
    boolean initiallyEnabled = stored.get(FEATURE_FLAG).equals("true");

    assertThat(scrapeFeatureFlag()).isEqualTo(initiallyEnabled ? "1.0" : "0.0");

    ImmutableMap.Builder<String, String> flipped = ImmutableMap.builder();
    stored.forEach(
        (name, value) ->
            flipped.put(
                name, name.equals(FEATURE_FLAG) ? String.valueOf(!initiallyEnabled) : value));
    assertThat(settingsService.updateSettings(flipped.build(), "metrics route test").updated())
        .isTrue();

    assertThat(scrapeFeatureFlag()).isEqualTo(initiallyEnabled ? "0.0" : "1.0");
  }

  private String scrapeFeatureFlag() {
    Http.RequestBuilder request =
        fakeRequestBuilder()
            .method("GET")
            .uri("/metrics")
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort());
    Result result = route(app, request);
    assertThat(result.status()).isEqualTo(200);

    Matcher matcher = FEATURE_FLAG_SERIES.matcher(contentAsString(result));
    assertThat(matcher.find()).isTrue();
    return matcher.group(1);
  }
}
