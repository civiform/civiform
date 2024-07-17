package support;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.itextpdf.xmp.impl.Base64;
import java.util.List;
import org.junit.Test;
import play.mvc.Http.Request;

public class FakeRequestBuilderTest {
  @Test
  public void createsFakeRequest() {
    String rawCreds = "raw creds";
    String encodedCreds = Base64.encode(rawCreds);

    Request fakeRequest =
        fakeRequestBuilder()
            .rawCredentials(rawCreds)
            .addXForwardedFor("4.4.4.4, 5.5.5.5")
            .addXForwardedFor("6.6.6.6")
            .build();

    assertThat(fakeRequest.header("Authorization").get()).isEqualTo("Basic " + encodedCreds);
    assertThat(fakeRequest.headers().getAll("X-Forwarded-For"))
        .isEqualTo(List.of("4.4.4.4, 5.5.5.5", "6.6.6.6"));
  }

  @Test
  public void hasUsableDefaults() {
    Request fakeRequest = fakeRequest();

    assertThat(fakeRequest.header("Authorization")).isEmpty();
    assertThat(fakeRequest.header("X-Forwarded-For")).isEmpty();
  }
}
