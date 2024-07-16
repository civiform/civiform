package support;

import static play.api.test.CSRFTokenHelper.addCSRFToken;

import auth.ClientIpResolver;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import play.mvc.Call;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;
import play.mvc.Http.RequestImpl;

public final class FakeRequestBuilder extends RequestBuilder {
  private List<String> xForwardedFor = new ArrayList<>();

  public static Request fakeRequestNew() {
    return new FakeRequestBuilder().build();
  }

  public static FakeRequestBuilder fakeRequestBuilder() {
    return new FakeRequestBuilder();
  }

  private FakeRequestBuilder() {
    addCSRFToken(this);
  }

  public FakeRequestBuilder call(Call call) {
    method(call.method());
    uri(call.url());
    return this;
  }

  /** Add an X-Forwarded-For header. Can be called multiple times for multiple header lines. */
  public FakeRequestBuilder addXForwardedFor(String xff) {
    this.xForwardedFor.add(xff);
    return this;
  }

  public FakeRequestBuilder rawCredentials(String rawCredentials) {
    String encodedCreds =
        Base64.getEncoder().encodeToString(rawCredentials.getBytes(StandardCharsets.UTF_8));
    header("Authorization", "Basic " + encodedCreds);
    return this;
  }

  @Override
  public RequestImpl build() {
    if (!xForwardedFor.isEmpty()) {
      // Each call to header() for a given key will override previous calls, so collect all the
      // values and set them once at the end
      header(ClientIpResolver.X_FORWARDED_FOR, xForwardedFor);
    }
    return super.build();
  }
}
