package filters;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeRequest;

import akka.stream.testkit.NoMaterializer$;
import com.google.common.collect.ImmutableList;
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

public class RecordCookieSizeFilterTest {

  @Before
  public void clearRegistry() {
    CollectorRegistry.defaultRegistry.clear();
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
    RecordCookieSizeFilter filter = new RecordCookieSizeFilter();

    // Run filter with several requests with various cookie sizes.
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
}
