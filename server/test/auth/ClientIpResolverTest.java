package auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import java.util.Optional;
import modules.ConfigurationException;
import org.junit.Test;
import org.mockito.Mockito;
import org.pac4j.play.PlayWebContext;
import play.mvc.Http.Request;
import services.settings.SettingsManifest;

public class ClientIpResolverTest {
  private static final SettingsManifest MOCK_SETTINGS_MANIFEST =
      Mockito.mock(SettingsManifest.class);

  @Test
  public void resolveClientIp_direct() {
    when(MOCK_SETTINGS_MANIFEST.getClientIpType()).thenReturn(Optional.of("DIRECT"));
    var clientIpResolver = new ClientIpResolver(MOCK_SETTINGS_MANIFEST);

    Request request =
        fakeRequestBuilder().addXForwardedFor("3.3.3.3, 2.2.2.2").remoteAddress("4.4.4.4").build();

    assertThat(clientIpResolver.resolveClientIp(new PlayWebContext(request))).isEqualTo("4.4.4.4");
  }

  @Test
  public void resolveClientIp_forwarded() {
    when(MOCK_SETTINGS_MANIFEST.getNumTrustedProxies()).thenReturn(Optional.of(1));
    when(MOCK_SETTINGS_MANIFEST.getClientIpType()).thenReturn(Optional.of("FORWARDED"));
    var clientIpResolver = new ClientIpResolver(MOCK_SETTINGS_MANIFEST);

    Request request = fakeRequestBuilder().addXForwardedFor("3.3.3.3, 2.2.2.2").build();

    assertThat(clientIpResolver.resolveClientIp(new PlayWebContext(request))).isEqualTo("2.2.2.2");
  }

  @Test
  public void resolveClientIp_forwarded_no_header() {
    when(MOCK_SETTINGS_MANIFEST.getNumTrustedProxies()).thenReturn(Optional.of(1));
    when(MOCK_SETTINGS_MANIFEST.getClientIpType()).thenReturn(Optional.of("FORWARDED"));
    var clientIpResolver = new ClientIpResolver(MOCK_SETTINGS_MANIFEST);

    Request request = fakeRequestBuilder().remoteAddress("3.3.3.3").build();

    assertThatThrownBy(() -> clientIpResolver.resolveClientIp(new PlayWebContext(request)))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage("CLIENT_IP_TYPE is FORWARDED but no value found for X-Forwarded-For header!");
  }

  @Test
  public void resolveClientIp_overload_singleProxy() {
    when(MOCK_SETTINGS_MANIFEST.getNumTrustedProxies()).thenReturn(Optional.of(1));
    when(MOCK_SETTINGS_MANIFEST.getClientIpType()).thenReturn(Optional.of("FORWARDED"));
    var clientIpResolver = new ClientIpResolver(MOCK_SETTINGS_MANIFEST);

    Request request = fakeRequestBuilder().addXForwardedFor("3.3.3.3, 2.2.2.2").build();

    assertThat(clientIpResolver.resolveClientIp(request)).isEqualTo("2.2.2.2");
  }

  @Test
  public void resolveClientIp_overload_multipleProxies_singleHeader() {
    when(MOCK_SETTINGS_MANIFEST.getNumTrustedProxies()).thenReturn(Optional.of(2));
    when(MOCK_SETTINGS_MANIFEST.getClientIpType()).thenReturn(Optional.of("FORWARDED"));
    var clientIpResolver = new ClientIpResolver(MOCK_SETTINGS_MANIFEST);

    Request request = fakeRequestBuilder().addXForwardedFor("3.3.3.3, 2.2.2.2, 1.1.1.1").build();

    assertThat(clientIpResolver.resolveClientIp(request)).isEqualTo("2.2.2.2");
  }

  @Test
  public void resolveClientIp_overload_multipleProxies_multipleHeaders() {
    when(MOCK_SETTINGS_MANIFEST.getNumTrustedProxies()).thenReturn(Optional.of(2));
    when(MOCK_SETTINGS_MANIFEST.getClientIpType()).thenReturn(Optional.of("FORWARDED"));
    var clientIpResolver = new ClientIpResolver(MOCK_SETTINGS_MANIFEST);

    Request request =
        fakeRequestBuilder()
            .addXForwardedFor("3.3.3.3, 2.2.2.2")
            .addXForwardedFor("1.1.1.1")
            .build();

    assertThat(clientIpResolver.resolveClientIp(request)).isEqualTo("2.2.2.2");
  }

  @Test
  public void resolveClientIp_overload_proxyConfigurationError() {
    when(MOCK_SETTINGS_MANIFEST.getNumTrustedProxies()).thenReturn(Optional.of(2));
    when(MOCK_SETTINGS_MANIFEST.getClientIpType()).thenReturn(Optional.of("FORWARDED"));
    var clientIpResolver = new ClientIpResolver(MOCK_SETTINGS_MANIFEST);

    Request request = fakeRequestBuilder().addXForwardedFor("1.1.1.1").build();

    assertThatThrownBy(() -> clientIpResolver.resolveClientIp(new PlayWebContext(request)))
        .isInstanceOf(ConfigurationException.class)
        .hasMessage(
            "The configured number of trusted proxies (2) is greater than the number of forwarding"
                + " hops (1)");
  }

  @Test
  public void getClientIpType_forwarded() {
    when(MOCK_SETTINGS_MANIFEST.getNumTrustedProxies()).thenReturn(Optional.of(1));
    when(MOCK_SETTINGS_MANIFEST.getClientIpType()).thenReturn(Optional.of("FORWARDED"));
    var clientIpResolver = new ClientIpResolver(MOCK_SETTINGS_MANIFEST);

    assertThat(clientIpResolver.getClientIpType()).isEqualTo(ClientIpType.FORWARDED);
  }

  @Test
  public void getClientIpType_direct() {
    when(MOCK_SETTINGS_MANIFEST.getNumTrustedProxies()).thenReturn(Optional.of(1));
    when(MOCK_SETTINGS_MANIFEST.getClientIpType()).thenReturn(Optional.of("DIRECT"));
    var clientIpResolver = new ClientIpResolver(MOCK_SETTINGS_MANIFEST);

    assertThat(clientIpResolver.getClientIpType()).isEqualTo(ClientIpType.DIRECT);
  }
}
