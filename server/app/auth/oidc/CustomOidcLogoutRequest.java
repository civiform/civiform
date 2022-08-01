package auth.oidc;

import com.google.common.base.Strings;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.util.MultivaluedMapUtils;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import com.nimbusds.openid.connect.sdk.LogoutRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomOidcLogoutRequest extends LogoutRequest {
  private static final Logger logger = LoggerFactory.getLogger(CustomOidcLogoutRequest.class);

  /** The optional post-logout redirection param and URI. */
  private final String postLogoutRedirectParam;

  private final URI postLogoutRedirectURI;

  private final Map<String, String> extraParams;

  /**
   * Create new OIDC logout request with a optional redirect url, optional client id, and other
   * params.
   */
  public CustomOidcLogoutRequest(
      final URI uri,
      final String postLogoutRedirectParam,
      final URI postLogoutRedirectURI,
      final Map<String, String> extraParams) {

    super(uri, /* idTokenHint */ null, postLogoutRedirectURI, /* state */ null);

    if (Strings.isNullOrEmpty(postLogoutRedirectParam)) {
      // default to OIDC spec
      this.postLogoutRedirectParam = "post_logout_redirect_uri";
    } else {
      this.postLogoutRedirectParam = postLogoutRedirectParam;
    }
    this.postLogoutRedirectURI = postLogoutRedirectURI;
    if (extraParams == null) {
      this.extraParams = Map.of();
    } else {
      this.extraParams = extraParams;
    }
  }

  /** Creates a new OIDC logout request without extra params or a custom postLogoutRedirectParam. */
  public CustomOidcLogoutRequest(final URI uri, final URI postLogoutRedirectURI) {

    this(uri, /* postLogoutRedirectParam */ null, postLogoutRedirectURI, /* extraParams */ null);
  }

  /** Creates a new OIDC logout request without a post-logout redirection. */
  public CustomOidcLogoutRequest(final URI uri, final Map<String, String> extraParams) {

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

  /**
   * Parses a logout request from the specified URI and query parameters.
   *
   * <p>Example parameters:
   *
   * <pre>
   * id_token_hint = eyJhbGciOiJSUzI1NiJ9.eyJpc3Mi...
   * post_logout_redirect_uri = https://client.example.com/post-logout
   * state = af0ifjsldkj
   * </pre>
   *
   * @param uri The URI of the end-session endpoint. May be {@code null} if the {@link
   *     #toHTTPRequest()} method will not be used.
   * @param params The parameters, empty map if none. Must not be {@code null}.
   * @return The logout request.
   * @throws ParseException If the parameters couldn't be parsed to a logout request.
   */
  public static LogoutRequest parse(final URI uri, final Map<String, List<String>> params)
      throws ParseException {

    logger.debug("Parse logout request called.  You should check this");
    String v = MultivaluedMapUtils.getFirstValue(params, "id_token_hint");

    JWT idTokenHint = null;

    if (StringUtils.isNotBlank(v)) {

      try {
        idTokenHint = JWTParser.parse(v);
      } catch (java.text.ParseException e) {
        throw new ParseException("Invalid ID token hint: " + e.getMessage(), e);
      }
    }

    v = MultivaluedMapUtils.getFirstValue(params, "post_logout_redirect_uri");

    URI postLogoutRedirectURI = null;

    if (StringUtils.isNotBlank(v)) {

      try {
        postLogoutRedirectURI = new URI(v);
      } catch (URISyntaxException e) {
        throw new ParseException(
            "Invalid \"post_logout_redirect_uri\" parameter: " + e.getMessage(), e);
      }
    }

    State state = null;

    v = MultivaluedMapUtils.getFirstValue(params, "state");

    if (postLogoutRedirectURI != null && StringUtils.isNotBlank(v)) {
      state = new State(v);
    }

    return new LogoutRequest(uri, idTokenHint, postLogoutRedirectURI, state);
  }
}
