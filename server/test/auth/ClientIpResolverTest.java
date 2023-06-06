package auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import org.pac4j.play.PlayWebContext;

public class ClientIpResolverTest {
  @Test
  public void resolveClientIp_direct() {
    var clientIpResolver =
        new ClientIpResolver(ConfigFactory.parseMap(ImmutableMap.of("client_ip_type", "DIRECT")));

    var request =
        new FakeRequestBuilder()
            .withRemoteAddress("4.4.4.4")
            .withXForwardedFor("3.3.3.3, 2.2.2.2")
            .build();

    assertThat(clientIpResolver.resolveClientIp(new PlayWebContext(request))).isEqualTo("4.4.4.4");
  }

  @Test
  public void resolveClientIp_forwarded() {
    var clientIpResolver =
        new ClientIpResolver(
            ConfigFactory.parseMap(ImmutableMap.of("client_ip_type", "FORWARDED")));

    var request = new FakeRequestBuilder().withXForwardedFor("3.3.3.3, 2.2.2.2").build();

    assertThat(clientIpResolver.resolveClientIp(new PlayWebContext(request))).isEqualTo("2.2.2.2");
  }

  @Test
  public void resolveClientIp_forwarded_no_header() {
    var clientIpResolver =
        new ClientIpResolver(
            ConfigFactory.parseMap(ImmutableMap.of("client_ip_type", "FORWARDED")));

    var request = new FakeRequestBuilder().withRemoteAddress("3.3.3.3").build();

    assertThatThrownBy(() -> clientIpResolver.resolveClientIp(new PlayWebContext(request)))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("CLIENT_IP_TYPE is FORWARDED but no value found for X-Forwarded-For header!");
  }

  @Test
  public void resolveClientIp_overload() {
    var clientIpResolver =
        new ClientIpResolver(
            ConfigFactory.parseMap(ImmutableMap.of("client_ip_type", "FORWARDED")));

    var request = new FakeRequestBuilder().withXForwardedFor("3.3.3.3, 2.2.2.2").build();

    assertThat(clientIpResolver.resolveClientIp(request)).isEqualTo("2.2.2.2");
  }

  @Test
  public void getClientIpType_forwarded() {
    var clientIpResolver =
        new ClientIpResolver(
            ConfigFactory.parseMap(ImmutableMap.of("client_ip_type", "FORWARDED")));

    assertThat(clientIpResolver.getClientIpType()).isEqualTo(ClientIpType.FORWARDED);
  }

  @Test
  public void getClientIpType_direct() {
    var clientIpResolver =
        new ClientIpResolver(ConfigFactory.parseMap(ImmutableMap.of("client_ip_type", "DIRECT")));

    assertThat(clientIpResolver.getClientIpType()).isEqualTo(ClientIpType.DIRECT);
  }
}
