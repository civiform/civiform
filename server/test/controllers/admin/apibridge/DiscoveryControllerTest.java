package controllers.admin.apibridge;

import static org.assertj.core.api.Assertions.assertThat;
import static play.inject.Bindings.bind;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import controllers.WithMockedProfiles;
import org.junit.Before;
import org.junit.Test;
import play.Environment;
import play.Mode;

public class DiscoveryControllerTest extends WithMockedProfiles {
  private DiscoveryController controller;
  private Config config =
      ConfigFactory.parseMap(
              ImmutableMap.<String, String>builder()
                  .put("api_bridge_enabled", "true")
                  .buildKeepingLast())
          .withFallback(ConfigFactory.load());

  @Before
  public void setup() {
    resetDatabase();
    createGlobalAdminWithMockedProfile();
    setupInjectorWithExtraBinding(bind(Config.class).toInstance(config));
    controller = instanceOf(DiscoveryController.class);
  }

  @Test
  public void discovery() {
    var request = fakeRequestBuilder().build();
    var response = controller.discovery(request);
    assertThat(response.status()).isEqualTo(200);
  }

  @Test
  public void hxDiscoveryPopulate_noform() {
    var request = fakeRequestBuilder().build();
    var response = controller.hxDiscoveryPopulate(request).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(200);
    assertThat(contentAsString(response)).contains("URL is required");
  }

  @Test
  public void hxDiscoveryPopulate_invalidUrl() {
    var request = fakeRequestBuilder().bodyForm(ImmutableMap.of("hostUrl", "vin.com")).build();
    var response = controller.hxDiscoveryPopulate(request).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(200);
    assertThat(contentAsString(response)).contains("enter a valid URL");
  }

  @Test
  public void hxDiscoveryPopulate_noEndpointsFound() {
    var request =
        fakeRequestBuilder().bodyForm(ImmutableMap.of("hostUrl", "http://vin.com")).build();
    var response = controller.hxDiscoveryPopulate(request).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(200);
    assertThat(contentAsString(response)).contains("No endpoints found");
  }

  @Test
  public void hxDiscoveryPopulate_prodModeRequiresHttps() {
    setupInjectorWithExtraBinding(
        bind(Config.class).toInstance(config),
        bind(Environment.class).toInstance(new Environment(Mode.PROD)));

    controller = instanceOf(DiscoveryController.class);

    var request =
        fakeRequestBuilder().bodyForm(ImmutableMap.of("hostUrl", "http://vin.com")).build();
    var response = controller.hxDiscoveryPopulate(request).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(200);
    assertThat(contentAsString(response)).contains("URL must start with HTTPS");
  }

  @Test
  public void hxDiscoveryPopulate_success() {
    var request =
        fakeRequestBuilder()
            .bodyForm(ImmutableMap.of("hostUrl", "http://mock-web-services:8000/api-bridge"))
            .build();
    var response = controller.hxDiscoveryPopulate(request).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(200);
    assertThat(contentAsString(response)).contains("Endpoint: /bridge/success");
  }

  @Test
  public void hxDiscoveryPopulate_noAvailableEndpoints() {
    var request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "hostUrl",
                    "http://mock-web-services:8000/api-bridge",
                    "urlPath",
                    "/bridge/success"))
            .build();
    var response = controller.hxAdd(request).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(200);
    assertThat(contentAsString(response)).contains("Saved successfully");

    request =
        fakeRequestBuilder()
            .bodyForm(ImmutableMap.of("hostUrl", "http://mock-web-services:8000/api-bridge"))
            .build();
    response = controller.hxDiscoveryPopulate(request).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(200);
    assertThat(contentAsString(response)).contains("No endpoints found");
  }

  @Test
  public void hxAdd_noform() {
    var request = fakeRequestBuilder().build();
    var response = controller.hxAdd(request).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(200);
    assertThat(contentAsString(response)).contains("URL is required").contains("Path is required");
  }

  @Test
  public void hxAdd_invalidHostUrl() {
    var request =
        fakeRequestBuilder()
            .bodyForm(ImmutableMap.of("hostUrl", "vin.com", "urlPath", "path"))
            .build();
    var response = controller.hxAdd(request).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(200);
    assertThat(contentAsString(response)).contains("enter a valid URL");
  }

  @Test
  public void hxAdd_prodModeRequiresHttps() {
    setupInjectorWithExtraBinding(
        bind(Config.class).toInstance(config),
        bind(Environment.class).toInstance(new Environment(Mode.PROD)));

    controller = instanceOf(DiscoveryController.class);

    var request =
        fakeRequestBuilder()
            .bodyForm(ImmutableMap.of("hostUrl", "http://vin.com", "urlPath", "path"))
            .build();
    var response = controller.hxAdd(request).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(200);
    assertThat(contentAsString(response)).contains("URL must start with HTTPS");
  }

  @Test
  public void hxAdd_success() {
    var request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "hostUrl",
                    "http://mock-web-services:8000/api-bridge",
                    "urlPath",
                    "/bridge/success"))
            .build();
    var response = controller.hxAdd(request).toCompletableFuture().join();
    assertThat(response.status()).isEqualTo(200);
    assertThat(contentAsString(response)).contains("Saved successfully");
  }
}
