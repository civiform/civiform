package auth;

import static play.test.Helpers.fakeRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import play.mvc.Http;

public final class FakeRequestBuilder {
  String remoteAddress = "1.1.1.1";
  Optional<String> xForwardedFor = Optional.empty();
  Optional<String> rawCredentials = Optional.empty();

  public FakeRequestBuilder() {}

  public FakeRequestBuilder withRemoteAddress(String remoteAddress) {
    this.remoteAddress = remoteAddress;
    return this;
  }

  public FakeRequestBuilder withXForwardedFor(String xForwardedFor) {
    this.xForwardedFor = Optional.of(xForwardedFor);
    return this;
  }

  public FakeRequestBuilder withRawCredentials(String rawCredentials) {
    this.rawCredentials = Optional.of(rawCredentials);
    return this;
  }

  public Http.Request build() {
    Http.RequestBuilder fakeRequest = fakeRequest().remoteAddress(this.remoteAddress);
    xForwardedFor.ifPresent(xForwardedFor -> fakeRequest.header("X-Forwarded-For", xForwardedFor));
    rawCredentials
        .map(
            rawCreds ->
                Base64.getEncoder().encodeToString(rawCreds.getBytes(StandardCharsets.UTF_8)))
        .ifPresent(creds -> fakeRequest.header("Authorization", "Basic " + creds));
    return fakeRequest.build();
  }
}
