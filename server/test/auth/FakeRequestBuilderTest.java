package auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.itextpdf.xmp.impl.Base64;
import org.junit.Test;
import play.mvc.Http;

public class FakeRequestBuilderTest {
  @Test
  public void createsFakeRequest() {
    String rawCreds = "raw creds";
    String encodedCreds = Base64.encode(rawCreds);

    Http.Request fakeRequest =
        new FakeRequestBuilder()
            .withRawCredentials(rawCreds)
            .withRemoteAddress("3.3.3.3")
            .withXForwardedFor("4.4.4.4, 5.5.5.5")
            .build();

    assertThat(fakeRequest.header("Authorization").get()).isEqualTo("Basic " + encodedCreds);
    assertThat(fakeRequest.remoteAddress()).isEqualTo("3.3.3.3");
    assertThat(fakeRequest.header("X-Forwarded-For").get()).isEqualTo("4.4.4.4, 5.5.5.5");
  }

  @Test
  public void hasUsableDefaults() {
    Http.Request fakeRequest = new FakeRequestBuilder().build();

    assertThat(fakeRequest.header("Authorization")).isEmpty();
    // Tests that don't care about the IP address assume a remote address of 1.1.1.1
    assertThat(fakeRequest.remoteAddress()).isEqualTo("1.1.1.1");
    assertThat(fakeRequest.header("X-Forwarded-For")).isEmpty();
  }
}
