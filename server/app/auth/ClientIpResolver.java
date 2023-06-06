package auth;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.typesafe.config.Config;
import javax.inject.Inject;
import org.pac4j.core.context.WebContext;
import org.pac4j.play.PlayWebContext;
import play.mvc.Http;

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
  private final ClientIpType clientIpType;

  @Inject
  public ClientIpResolver(Config config) {
    this.clientIpType = checkNotNull(config).getEnum(ClientIpType.class, "client_ip_type");
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
   * @see #resolveClientIp(WebContext)
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
  public String resolveClientIp(WebContext context) {
    switch (clientIpType) {
      case DIRECT:
        return context.getRemoteAddr();
      case FORWARDED:
        String forwardedFor =
            context
                .getRequestHeader("X-Forwarded-For")
                .orElseThrow(
                    () ->
                        new RuntimeException(
                            "CLIENT_IP_TYPE is FORWARDED but no value found for X-Forwarded-For"
                                + " header!"));
        // AWS appends the original client IP to the end of the X-Forwarded-For
        // header if it is present in the original request.
        // See
        // https://docs.aws.amazon.com/elasticloadbalancing/latest/application/x-forwarded-headers.html
        return Iterables.getLast(Splitter.on(",").split(forwardedFor)).strip();
      default:
        throw new IllegalStateException(
            String.format("Unrecognized ClientIpType: %s", clientIpType));
    }
  }
}
