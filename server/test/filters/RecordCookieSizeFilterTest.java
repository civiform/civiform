package filters;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeRequest;

import akka.stream.testkit.NoMaterializer$;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import io.prometheus.client.CollectorRegistry;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import services.settings.SettingDescription;
import services.settings.SettingMode;
import services.settings.SettingType;
import services.settings.SettingsManifest;
import services.settings.SettingsSection;

public class RecordCookieSizeFilterTest {
  @Before
  public void clearRegistry() {
    CollectorRegistry.defaultRegistry.clear();
  }

  private SettingsManifest createSettingsManifest(boolean metricsEnabled) {
    return new SettingsManifest(
        ImmutableMap.of(
            "section1",
            SettingsSection.create(
                "section1",
                "description1",
                ImmutableList.of(),
                ImmutableList.of(
                    SettingDescription.create(
                        "CIVIFORM_SERVER_METRICS_ENABLED",
                        "CiviForm server metrics enabled",
                        false,
                        SettingType.BOOLEAN,
                        SettingMode.ADMIN_WRITEABLE)))),
        ConfigFactory.parseMap(ImmutableMap.of("civiform_server_metrics_enabled", metricsEnabled)));
  }

  private Result runFilterWithCookieSize(RecordCookieSizeFilter filter, int cookieSize)
      throws Exception {
    Http.Cookie playSessionCookie =
        Http.Cookie.builder("PLAY_SESSION", "*".repeat(cookieSize)).build();
    Http.Request request = fakeRequest().cookie(playSessionCookie).build();
    return filter
        .apply(EssentialAction.of(r -> Accumulator.done(Results.ok("OK"))))
        .apply(request)
        .run(NoMaterializer$.MODULE$)
        .toCompletableFuture()
        .get();
  }

  private double samplesBelowUpperBound(List<Integer> sizes, double upperBound) {
    return sizes.stream().filter(n -> n <= upperBound).count();
  }

  @Test
  public void testCookieSizesAreRecorded() throws Exception {
    // Run filter with several requests with various cookie sizes.
    RecordCookieSizeFilter filter = new RecordCookieSizeFilter(() -> createSettingsManifest(true));

    List<Integer> cookieSizes = ImmutableList.of(1000, 2000, 2000, 3000, 3500, 4000);
    for (int cookieSize : cookieSizes) {
      assertThat(runFilterWithCookieSize(filter, cookieSize).status()).isEqualTo(200);
    }

    var metricFamilySamplesEnumeration = CollectorRegistry.defaultRegistry.metricFamilySamples();
    assertThat(metricFamilySamplesEnumeration.hasMoreElements()).isTrue();
    var metricFamilySamples = metricFamilySamplesEnumeration.nextElement();
    assertThat(metricFamilySamples.name).isEqualTo("play_session_cookie_size_bytes");

    double upperBoundOfCookieSize = 0;
    for (var sample : metricFamilySamples.samples) {
      if (sample.name.equals("play_session_cookie_size_bytes_count")) {
        // Number of samples.
        assertThat(sample.value).isEqualTo(cookieSizes.size());
        break;
      }
      if (sample.name.equals("play_session_cookie_size_bytes_sum")) {
        // The value of the sum is the sum of sample values.
        assertThat(sample.value)
            .isEqualTo(cookieSizes.stream().collect(Collectors.summingInt(Integer::intValue)));
        break;
      }
      // The remaining samples are histogram buckets.
      assertThat(sample.name).isEqualTo("play_session_cookie_size_bytes_bucket");
      // Buckets have a label with name "le" and a value indicating the upper bound of the bucket.
      assertThat(sample.labelNames).contains("le");
      if (upperBoundOfCookieSize
          < RecordCookieSizeFilter.NUM_BUCKETS * RecordCookieSizeFilter.BUCKET_SIZE) {
        assertThat(sample.labelValues).contains(String.valueOf(upperBoundOfCookieSize));
        // The value of the sample is the cumulative number of samples in the current bucket or
        // below.
        assertThat(sample.value)
            .isEqualTo(samplesBelowUpperBound(cookieSizes, upperBoundOfCookieSize));
      } else {
        // The last bucket has a special label.
        assertThat(sample.labelValues).contains("+Inf");
        assertThat(sample.value).isEqualTo(cookieSizes.size());
      }
      upperBoundOfCookieSize += RecordCookieSizeFilter.BUCKET_SIZE;
    }
  }

  @Test
  public void testCookieSizesAreNotRecordedIfMetricsAreNotEnabled() throws Exception {
    RecordCookieSizeFilter filter = new RecordCookieSizeFilter(() -> createSettingsManifest(false));

    assertThat(runFilterWithCookieSize(filter, 1000).status()).isEqualTo(200);

    var metricFamilySamplesEnumeration = CollectorRegistry.defaultRegistry.metricFamilySamples();
    assertThat(metricFamilySamplesEnumeration.hasMoreElements()).isTrue();
    var metricFamilySamples = metricFamilySamplesEnumeration.nextElement();
    assertThat(metricFamilySamples.name).isEqualTo("play_session_cookie_size_bytes");

    assertThat(metricFamilySamples.samples.isEmpty()).isTrue();
  }
}
