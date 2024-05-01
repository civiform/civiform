package auth;

import static play.test.Helpers.fakeRequest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import play.mvc.Http;

public final class FakeRequestBuilder {
  private String remoteAddress = "1.1.1.1";
  private List<String> xForwardedFor = new ArrayList<>();
  private Optional<String> rawCredentials = Optional.empty();

  public FakeRequestBuilder() {}

  public FakeRequestBuilder withRemoteAddress(String remoteAddress) {
    this.remoteAddress = remoteAddress;
    return this;
  }

  // Add an X-Forwarded-For header.
  //
  // Can be called multiple times.
  public FakeRequestBuilder withXForwardedFor(String xff) {
    this.xForwardedFor.add(xff);
    return this;
  }

  public FakeRequestBuilder withRawCredentials(String rawCredentials) {
    this.rawCredentials = Optional.of(rawCredentials);
    return this;
  }

  public Http.Request build() {
    Http.RequestBuilder fakeRequest = fakeRequest().remoteAddress(this.remoteAddress);
    if (!xForwardedFor.isEmpty()) {
      fakeRequest.header(ClientIpResolver.X_FORWARDED_FOR, xForwardedFor);
    }
    rawCredentials
        .map(
            rawCreds ->
                Base64.getEncoder().encodeToString(rawCreds.getBytes(StandardCharsets.UTF_8)))
        .ifPresent(creds -> fakeRequest.header("Authorization", "Basic " + creds));
    return fakeRequest.build();
  }
}
