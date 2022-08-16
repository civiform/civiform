package auth.oidc;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.nimbusds.openid.connect.sdk.LogoutRequest;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

  /** Pptional extra query params to add to the URL. */
  private final ImmutableMap<String, String> extraParams;

  /**
   * Create new OIDC logout request with a optional redirect url, optional client id, and other
   * params.
   */
  public CustomOidcLogoutRequest(
      final URI uri,
      final String postLogoutRedirectParam,
      final URI postLogoutRedirectURI,
      final ImmutableMap<String, String> extraParams) {

    super(uri, /* idTokenHint */ null, postLogoutRedirectURI, /* state */ null);

    if (Strings.isNullOrEmpty(postLogoutRedirectParam)) {
      // default to OIDC spec
      this.postLogoutRedirectParam = "post_logout_redirect_uri";
    } else {
      this.postLogoutRedirectParam = postLogoutRedirectParam;
    }
    this.postLogoutRedirectURI = postLogoutRedirectURI;
    if (extraParams == null) {
      this.extraParams = ImmutableMap.of();
    } else {
      this.extraParams = extraParams;
    }
  }

  /** Creates a new OIDC logout request without extra params or a custom postLogoutRedirectParam. */
  public CustomOidcLogoutRequest(final URI uri, final URI postLogoutRedirectURI) {

    this(uri, /* postLogoutRedirectParam */ null, postLogoutRedirectURI, /* extraParams */ null);
  }

  /** Creates a new OIDC logout request without a post-logout redirection. */
  public CustomOidcLogoutRequest(final URI uri, final ImmutableMap<String, String> extraParams) {

    this(uri, /* postLogoutRedirectParam */ null, /* postLogoutRedirectURI */ null, extraParams);
  }

  /** Creates a new OIDC logout request without extra params. */
  public CustomOidcLogoutRequest(
      final URI uri, final String postLogoutRedirectParam, final URI postLogoutRedirectURI) {

    this(uri, postLogoutRedirectParam, postLogoutRedirectURI, /* extraParams */ null);
  }

  /** Creates a new OIDC logout request without a post-logout redirection or extra params. */
  public CustomOidcLogoutRequest(final URI uri) {

    this(
        uri, /* postLogoutRedirectParam */ null, /* postLogoutRedirectURI */ null, /* extraParams */
        null);
  }

  /** Returns the URI query parameters for this logout request. */
  @Override
  public Map<String, List<String>> toParameters() {

    Map<String, List<String>> params = new LinkedHashMap<>();

    if (postLogoutRedirectURI != null) {
      params.put(
          postLogoutRedirectParam, Collections.singletonList(postLogoutRedirectURI.toString()));
    }

    extraParams.forEach((key, value) -> params.put(key, Collections.singletonList(value)));

    return params;
  }
}
