package support;

import static services.settings.SettingsService.CIVIFORM_SETTINGS_ATTRIBUTE_KEY;

import auth.ClientIpResolver;
import com.google.common.collect.ImmutableMap;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import play.api.mvc.request.RequestAttrKey;
import play.api.routing.HandlerDef;
import play.mvc.Call;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;
import play.mvc.Http.RequestImpl;
import scala.jdk.javaapi.CollectionConverters;

public final class FakeRequestBuilder extends RequestBuilder {
  private List<String> xForwardedFor = new ArrayList<>();
  private ImmutableMap.Builder<String, String> settingsMap = ImmutableMap.builder();

  public static Request fakeRequest() {
    return fakeRequestBuilder().build();
  }

  public static FakeRequestBuilder fakeRequestBuilder() {
    return new FakeRequestBuilder().addCSRFToken().cspNonce("do-not-assert-on-this-value-in-tests");
  }

  private FakeRequestBuilder() {}

  public FakeRequestBuilder call(Call call) {
    method(call.method());
    uri(call.url());
    return this;
  }

  public FakeRequestBuilder location(String httpVerb, String path) {
    method(httpVerb);
    uri(path);
    return this;
  }

  public FakeRequestBuilder addCSRFToken() {
    play.api.test.CSRFTokenHelper.addCSRFToken(this);
    return this;
  }

  /** Add an entry in the CiviForm settings map. Can be called multiple times. */
  public FakeRequestBuilder addCiviFormSetting(String key, String value) {
    settingsMap.put(key, value);
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

  public FakeRequestBuilder cspNonce(String nonce) {
    attr(RequestAttrKey.CSPNonce().asJava(), nonce);
    return this;
  }

  @Override
  public RequestImpl build() {
    if (!xForwardedFor.isEmpty()) {
      // Each call to header() for a given key will override previous calls, so collect all the
      // values and set them once at the end
      header(ClientIpResolver.X_FORWARDED_FOR, xForwardedFor);
    }
    ImmutableMap<String, String> settings = settingsMap.build();
    if (!settings.isEmpty()) {
      attr(CIVIFORM_SETTINGS_ATTRIBUTE_KEY, settings);
    }
    return super.build();
  }

  /**
   * Create a {@link HandlerDef} object with customized route information
   *
   * @param routePattern is the pattern for the routes
   * @return {@link HandlerDef}
   */
  public static HandlerDef createHandlerDef(ClassLoader classLoader, String routePattern) {
    HandlerDef handlerDef =
        new HandlerDef(
            classLoader,
            "router",
            "controllers.MyFakeController", // This can be anything
            "index",
            CollectionConverters.asScala(Collections.<Class<?>>emptyList()).toSeq(),
            "GET",
            routePattern,
            "",
            CollectionConverters.asScala(Collections.<String>emptyList()).toSeq());

    return handlerDef;
  }
}
