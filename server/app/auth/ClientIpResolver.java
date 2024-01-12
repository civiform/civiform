package auth;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import javax.inject.Inject;
import modules.ConfigurationException;
import org.pac4j.play.PlayWebContext;
import play.mvc.Http;
import services.settings.SettingsManifest;

/**
 * Resolves the client IP address from the provided request.
 *
 * <p>The value of the {@link ClientIpType} config variable is used to determine where to look for
 * the client's IP address.
 *
 * <p>When CiviForm is behind a load balancer the client's IP address is not in the remoteAddress
 * field of the request. To find the actual client IP we need to inspect the {@code X-Forwarded-For}
 * header that's added by the load balancer. See MDN's <a
 * href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-For#selecting_an_ip_address">X-Forwarded-For
 * docs</a> for more.
 */
public final class ClientIpResolver {
  public static final String X_FORWARDED_FOR = "X-Forwarded-For";
  private final SettingsManifest settingsManifest;
  private final ClientIpType clientIpType;

  @Inject
  public ClientIpResolver(SettingsManifest settingsManifest) {
    this.settingsManifest = checkNotNull(settingsManifest);
    if (settingsManifest.getClientIpType().isEmpty()) {
      throw new ConfigurationException("CLIENT_IP_TYPE is not configured");
    }
    this.clientIpType = ClientIpType.valueOf(settingsManifest.getClientIpType().orElseThrow());
  }

  /**
   * Gets the current {@link ClientIpType}
   *
   * @return the value of the {@link ClientIpType} config variable
   */
  public ClientIpType getClientIpType() {
    return this.clientIpType;
  }

  /**
   * Resolve the client's IP, using {@code X-Forwarded-For} if the {@link ClientIpType} is set to
   * {@code FORWARDED}.
   *
   * @param request - the request to parse for the client IP address
   * @see #resolveClientIp(PlayWebContext)
   */
  public String resolveClientIp(Http.RequestHeader request) {
    return resolveClientIp(new PlayWebContext(request));
  }

  /**
   * Resolve the client's IP, using {@code X-Forwarded-For} if the {@link ClientIpType} is set to
   * {@code FORWARDED}.
   *
   * @param context the WebContext for the request
   * @return the resolved IP address for the client
   */
  public String resolveClientIp(PlayWebContext context) {

    switch (clientIpType) {
      case DIRECT:
        return context.getRemoteAddr();
      case FORWARDED:
        return getClientIpFromRequest(context);
      default:
        throw new IllegalStateException(
            String.format("Unrecognized ClientIpType: %s", clientIpType));
    }
  }

  private String getClientIpFromRequest(PlayWebContext context) {
    int numTrustedProxies =
        settingsManifest
            .getNumTrustedProxies()
            .orElseThrow(() -> new ConfigurationException("NUM_TRUSTED_PROXIES is not configured"));

    // AWS appends the original client IP to the end of the X-Forwarded-For
    // header if it is present in the original request.
    // See
    // https://docs.aws.amazon.com/elasticloadbalancing/latest/application/x-forwarded-headers.html

    Http.Headers headers = context.getNativeJavaRequest().headers();
    if (!headers.contains(X_FORWARDED_FOR)) {
      throw new ConfigurationException(
          "CLIENT_IP_TYPE is FORWARDED but no value found for X-Forwarded-For header!");
    }

    ArrayList<String> ips = new ArrayList<>();
    for (String xffHeader : headers.getAll(X_FORWARDED_FOR)) {
      Splitter.on(',').trimResults().omitEmptyStrings().split(xffHeader).forEach(ips::add);
    }
    int numIps = ips.size();

    if (numTrustedProxies > numIps) {
      throw new ConfigurationException(
          String.format(
              "The configured number of trusted proxies (%d) is greater than the number of"
                  + " forwarding hops (%d)",
              numTrustedProxies, numIps));
    }

    return ips.get(numIps - numTrustedProxies);
  }
}
