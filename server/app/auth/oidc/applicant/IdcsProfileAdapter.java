package auth.oidc.applicant;

import auth.ProfileFactory;
import com.google.common.collect.ImmutableMap;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.Resource;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Provider;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.UserRepository;

/**
 * This class takes an existing CiviForm profile and augments it with the information from an IDCS
 * profile.
 */
public class IdcsProfileAdapter extends OidcApplicantProfileAdapter {
  public static final Logger logger = LoggerFactory.getLogger(IdcsProfileAdapter.class);

  public IdcsProfileAdapter(
      OidcConfiguration configuration,
      OidcClient client,
      ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider) {
    super(
        configuration,
        client,
        /* app_configuration= */ null,
        profileFactory,
        applicantRepositoryProvider);
  }

  // Manually specify all app_configuration params.
  @Override
  protected String attributePrefix() {
    return "idcs";
  }
  ;

  @Override
  protected String getEmailAttributeName() {
    return "user_emailid";
  }

  @Override
  protected String getLocaleAttributeName() {
    return "user_locale";
  }

  @Override
  protected String getFirstNameAttributeName() {
    return "";
  }

  @Override
  protected String getSecondNameAttributeName() {
    return "user_displayname";
  }

  @Override
  protected String getConfigurationValue(String attr, String defaultValue) {
    return "";
  }

  @Override
  protected void possiblyModifyConfigBasedOnCred(Credentials cred) {
    // The flow here is not immediately intuitive. IDCS is to blame. :) The normal
    // flow for authenticating a user is "get user's data via POST. Decode it, check
    // that it is signed, and use it." IDCS throws in an extra step here - in order
    // to get IDCS's signing key, we need to provide an Authorization header proving
    // that we have a good reason to use the signing key. Pac4j and associated tools
    // are not well-suited to that, because it's a deviation from the OIDC spec.
    // Pac4j has the concept of a "resource retriever", which is used to fetch
    // things like the signing key. They are meant to be configured once and used
    // indefinitely, but we only get the access token at the time the user logs in
    // and is redirected to our server. So, we need to slighly abuse the notion of a
    // resource retriever. We create our own modified resource retriever which has
    // access to the required token.

    if (((OidcCredentials) cred).getAccessToken() == null) {
      logger.debug("No access token in the credentials.");
      return;
    }

    if (this.configuration.getResourceRetriever() instanceof CachedResourceRetriever) {
      logger.debug("Already have jwk cached.");
      return;
    }

    try {
      URI jwkSetUri = this.configuration.getProviderMetadata().getJWKSetURI();
      ImmutableMap<URI, Resource> jwkCache =
          ImmutableMap.of(
              jwkSetUri,
              new CredentialedResourceRetriever(configuration, cred)
                  .retrieveResource(jwkSetUri.toURL()));
      this.configuration.setResourceRetriever(new CachedResourceRetriever(configuration, jwkCache));
    } catch (IOException | NullPointerException e) {
      logger.error("Failed to fetch JWK", e);
    }
  }

  private static class CredentialedResourceRetriever extends DefaultResourceRetriever {
    private final Credentials cred;

    public CredentialedResourceRetriever(OidcConfiguration configuration, Credentials cred) {
      super(configuration.getConnectTimeout(), configuration.getReadTimeout());
      this.cred = cred;
    }

    @Override
    public Map<String, List<String>> getHeaders() {
      Map<String, List<String>> headers = super.getHeaders();
      if (headers == null) {
        headers = new HashMap<>();
      }
      String authHeader = ((OidcCredentials) cred).getAccessToken().toAuthorizationHeader();
      logger.debug("Auth header in the resource retriever: {}", authHeader);
      headers.put("Authorization", List.of(authHeader));
      return headers;
    }
  }

  private static class CachedResourceRetriever extends DefaultResourceRetriever {
    private final ImmutableMap<URI, Resource> resources;

    public CachedResourceRetriever(
        OidcConfiguration configuration, ImmutableMap<URI, Resource> resources) {
      super(configuration.getConnectTimeout(), configuration.getReadTimeout());
      this.resources = resources;
    }

    @Override
    public Resource retrieveResource(URL url) throws IOException {
      logger.debug("Attempting to fetch {}", url.toString());
      try {
        if (resources.containsKey(url.toURI())) {
          logger.debug("Cached: {}", url.toString());
          return resources.get(url.toURI());
        }
      } catch (URISyntaxException e) {
        logger.debug("failed to convert to URI", e);
      }
      return super.retrieveResource(url);
    }
  }
}
