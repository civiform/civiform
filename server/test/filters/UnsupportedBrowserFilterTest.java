package filters;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableList;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.pekko.stream.testkit.NoMaterializer$;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

@RunWith(JUnitParamsRunner.class)
public class UnsupportedBrowserFilterTest {

  private final UnsupportedBrowserFilter filter = new UnsupportedBrowserFilter();
  private final String unsupportedBrowserPath =
      controllers.routes.SupportController.handleUnsupportedBrowser().path();

  private static final String IE11 =
      "Mozilla/5.0 (Windows NT 10.0; Trident/7.0; rv:11.0) like Gecko";
  private static final String IE10 =
      "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; Trident/6.0)";
  private static final String IE9 =
      "Mozilla/4.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)";
  private static final String EDGE =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)"
          + " Chrome/103.0.0.0 Safari/537.36 Edg/103.0.1264.49";
  private static final String FIREFOX =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:102.0) Gecko/20100101 Firefox/102.0";
  private static final String CHROME =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)"
          + " Chrome/103.0.0.0 Safari/537.36";
  private static final String SAFARI =
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 12_4) AppleWebKit/605.1.15 (KHTML, like Gecko)"
          + " Version/15.4 Safari/605.1.15";

  @Test
  public void testMissingUserAgentDoesNotTriggerRedirect() throws Exception {
    Result res = runFilter(fakeRequestBuilder(), Results.ok(""));
    assertThat(res.status()).isEqualTo(200);
  }

  @Test
  @Parameters(method = "getUnsupportedBrowsers")
  public void testOldIETriggerRedirect(String userAgent) throws Exception {
    Result res =
        runFilter(
            fakeRequestBuilder().header(Http.HeaderNames.USER_AGENT, userAgent), Results.ok(""));
    assertThat(res.status()).isEqualTo(303);
    assertThat(res.headers()).containsEntry(Http.HeaderNames.LOCATION, unsupportedBrowserPath);
  }

  private ImmutableList<String> getUnsupportedBrowsers() {
    return ImmutableList.of(IE9, IE10, IE11);
  }

  @Test
  @Parameters(method = "getSupportedBrowsers")
  public void testModernBrowsersPassThrough(String userAgent) throws Exception {
    Result res =
        runFilter(
            fakeRequestBuilder().header(Http.HeaderNames.USER_AGENT, userAgent), Results.ok(""));
    assertThat(res.status()).isEqualTo(200);
  }

  private ImmutableList<String> getSupportedBrowsers() {
    return ImmutableList.of(EDGE, FIREFOX, CHROME, SAFARI);
  }

  @Test
  public void testRequestToUnsupportedBrowserPageIsNotRedirected() throws Exception {
    Result res =
        runFilter(
            fakeRequestBuilder()
                .header(Http.HeaderNames.USER_AGENT, IE11)
                .path(unsupportedBrowserPath),
            Results.ok(""));
    assertThat(res.status()).isEqualTo(200);
  }

  @Test
  public void testAssertsAreNotRedirected() throws Exception {
    Result res =
        runFilter(
            fakeRequestBuilder().header(Http.HeaderNames.USER_AGENT, IE11).path("/assets/foo.js"),
            Results.ok(""));
    assertThat(res.status()).isEqualTo(200);
  }

  private Result runFilter(Http.RequestBuilder request, Result defaultResult) throws Exception {
    return filter
        .apply(EssentialAction.of(r -> Accumulator.done(defaultResult)))
        .apply(request.build())
        .run(NoMaterializer$.MODULE$)
        .toCompletableFuture()
        .get();
  }
}
