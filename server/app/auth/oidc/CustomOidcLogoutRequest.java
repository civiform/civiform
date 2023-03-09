package auth.oidc;

import com.google.common.collect.ImmutableMap;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.LogoutRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.pac4j.core.exception.TechnicalException;

/**
 * Custom Logout Request that allows for divergence from the [oidc
 * spec](https://openid.net/specs/openid-connect-rpinitiated-1_0.html) if your provider requires it
 * (e.g. Auth0).
 *
 * <p>Does not provide the recommended id_token_hint, since these are not stored by the civiform
 * profile.
 *
 * <p>Uses the post_logout_redirect_uri parameter by default, but allows overriding to a different
 * value
 *
 * <p>Allows adding extra custom query parameters to the URL.
 */
public final class CustomOidcLogoutRequest extends LogoutRequest {
  /** The optional post-logout redirection query param. */
  private final String postLogoutRedirectParam;

  /** The optional post-logout redirection URI. */
  private final URI postLogoutRedirectURI;

  /** Optional extra query params to add to the URL. */
  private final ImmutableMap<String, String> extraParams;

  /**
   * Create new OIDC logout request with a optional redirect url, optional client id, and other
   * params. If the OIDC provider requires the optional state param for logout (see
   * https://openid.net/specs/openid-connect-rpinitiated-1_0.html), include it here. Note that the
   * state here is not saved and validated by the client, so it does not achieve the goal of
   * "maintain state between the logout request and the callback" as specified by the spec.
   */
  public CustomOidcLogoutRequest(
      final URI uri,
      final String postLogoutRedirectParam,
      final URI postLogoutRedirectURI,
      final ImmutableMap<String, String> extraParams,
      final State state) {

    super(uri, /* idTokenHint */ null, postLogoutRedirectURI, state);
    this.postLogoutRedirectParam = postLogoutRedirectParam;
    this.postLogoutRedirectURI = postLogoutRedirectURI;
    if (extraParams == null) {
      this.extraParams = ImmutableMap.of();
    } else {
      this.extraParams = extraParams;
    }
  }

  /** Returns the URI query parameters for this logout request. */
  @Override
  public Map<String, List<String>> toParameters() {

    Map<String, List<String>> params = super.toParameters();

    // Remove post_logout_redirect_uri and replace with custom logic.
    params.remove("post_logout_redirect_uri");
    if (postLogoutRedirectURI != null && !postLogoutRedirectParam.isEmpty()) {
      params.put(
          postLogoutRedirectParam, Collections.singletonList(postLogoutRedirectURI.toString()));
    }

    extraParams.forEach((key, value) -> params.put(key, Collections.singletonList(value)));

    return params;
  }

  @Override
  public URI toURI() {
    URI uri = super.toURI();
    // default behavior of LogoutRequest.toURI() removes fragment from the URI.
    // For some usecases (e.g. IDCS on Seattle) they use logout URI that contains
    // fragment and read it client-side. Here we add fragment back if it was
    // present in the original logout URI.
    if (getEndpointURI().getRawFragment() != null) {
      try {
        return new URI(uri + "#" + getEndpointURI().getRawFragment());
      } catch (URISyntaxException e) {
        throw new TechnicalException(e);
      }
    }
    return uri;
  }
}
