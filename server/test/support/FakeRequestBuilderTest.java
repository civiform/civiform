package support;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.itextpdf.xmp.impl.Base64;
import java.util.List;
import org.junit.Test;
import play.mvc.Call;
import play.mvc.Http.Request;
import views.html.helper.CSRF;

public class FakeRequestBuilderTest {
  @Test
  public void addXForwardedFor_collectsMultipleHeaders() {
    Request fakeRequest =
        fakeRequestBuilder()
            .addXForwardedFor("4.4.4.4, 5.5.5.5")
            .addXForwardedFor("6.6.6.6")
            .build();

    assertThat(fakeRequest.headers().getAll("X-Forwarded-For"))
        .isEqualTo(List.of("4.4.4.4, 5.5.5.5", "6.6.6.6"));
  }

  @Test
  public void rawCredentials_setsAuthorizationHeader() {
    String rawCreds = "raw creds";
    String encodedCreds = Base64.encode(rawCreds);
    Request fakeRequest = fakeRequestBuilder().rawCredentials(rawCreds).build();

    assertThat(fakeRequest.header("Authorization").get()).isEqualTo("Basic " + encodedCreds);
  }

  @Test
  public void call_setsMethodAndUri() {
    Call call = new play.api.mvc.Call("method", "url", null);
    Request fakeRequest = fakeRequestBuilder().call(call).build();

    assertThat(fakeRequest.method()).isEqualTo("method");
    assertThat(fakeRequest.uri()).isEqualTo("url");
  }

  @Test
  public void fakeRequest_hasCsrfToken() {
    Request fakeRequest = fakeRequest();

    assertThat(CSRF.getToken(fakeRequest.asScala()).value()).isNotEmpty();
  }
}
