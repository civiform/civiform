package support;

import auth.ClientIpResolver;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import play.mvc.Http.RequestBuilder;
import play.mvc.Http.RequestImpl;

public final class FakeRequestBuilder extends RequestBuilder {
  private List<String> xForwardedFor = new ArrayList<>();

  public FakeRequestBuilder() {}

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
