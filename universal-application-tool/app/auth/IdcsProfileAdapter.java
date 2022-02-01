package auth;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.Resource;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Provider;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.pac4j.oidc.profile.OidcProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.UserRepository;

/**
 * This class takes an existing CiviForm profile and augments it with the information from an IDCS
 * profile.
 */
public class IdcsProfileAdapter extends CiviFormProfileAdapter {
  public static final Logger LOG = LoggerFactory.getLogger(IdcsProfileAdapter.class);

  public IdcsProfileAdapter(
      OidcConfiguration configuration,
      OidcClient client,
      ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider) {
    super(configuration, client, profileFactory, applicantRepositoryProvider);
  }

  @Override
  protected String emailAttributeName() {
    return "user_emailid";
  }

  @Override
  protected ImmutableSet<Roles> roles(CiviFormProfile profile, OidcProfile oidcProfile) {
    if (profile.getAccount().join().getMemberOfGroup().isPresent()) {
      return ImmutableSet.of(Roles.ROLE_APPLICANT, Roles.ROLE_TI);
    }
    return ImmutableSet.of(Roles.ROLE_APPLICANT);
  }

  @Override
  protected void adaptForRole(CiviFormProfile profile, ImmutableSet<Roles> roles) {
    // not needed
  }

  @Override
  public CiviFormProfileData mergeCiviFormProfile(
      CiviFormProfile civiformProfile, OidcProfile oidcProfile) {
    final String locale = oidcProfile.getAttribute("user_locale", String.class);
    final boolean hasLocale = locale != null && !locale.isEmpty();
    final String displayName = oidcProfile.getAttribute("user_displayname", String.class);
    final boolean hasDisplayName = displayName != null && !displayName.isEmpty();
    if (hasLocale || hasDisplayName) {
      civiformProfile
          .getApplicant()
          .thenApplyAsync(
              applicant -> {
                if (hasLocale) {
                  applicant.getApplicantData().setPreferredLocale(Locale.forLanguageTag(locale));
                }
                if (hasDisplayName) {
                  applicant.getApplicantData().setUserName(displayName);
                }
                applicant.save();
                return null;
              })
          .toCompletableFuture()
          .join();
    }

    return super.mergeCiviFormProfile(civiformProfile, oidcProfile);
  }

  @Override
  public CiviFormProfileData civiformProfileFromOidcProfile(OidcProfile profile) {
    return mergeCiviFormProfile(
        profileFactory.wrapProfileData(profileFactory.createNewApplicant()), profile);
  }

  @Override
  protected void possiblyModifyConfigBasedOnCred(Credentials cred) {
    // The flow here is not immediately intuitive.  IDCS is to blame.  :)
    // The normal flow for authenticating a user is "get user's data via POST.
    // Decode it, check that it is signed, and use it." IDCS throws in an extra step
    // here - in order to get IDCS's signing key, we need to provide an Authorization
    // header proving that we have a good reason to use the signing key.
    // Pac4j and associated tools are not well-suited to that, because it's
    // a deviation from the OIDC spec.  Pac4j has the concept of a "resource retriever",
    // which is used to fetch things like the signing key.  They are meant to
    // be configured once and used indefinitely, but we only get the access
    // token at the time the user logs in and is redirected to our server.  So,
    // we need to slighly abuse the notion of a resource retriever.  We create our
    // own modified resource retriever which has access to the required token.

    if (((OidcCredentials) cred).getAccessToken() == null) {
      LOG.debug("No access token in the credentials.");
      return;
    }

    if (this.configuration.getResourceRetriever() instanceof CachedResourceRetriever) {
      LOG.debug("Already have jwk cached.");
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
      LOG.error("Failed to fetch JWK", e);
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
      LOG.debug("Auth header in the resource retriever: {}", authHeader);
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
      LOG.debug("Attempting to fetch {}", url.toString());
      try {
        if (resources.containsKey(url.toURI())) {
          LOG.debug("Cached: {}", url.toString());
          return resources.get(url.toURI());
        }
      } catch (URISyntaxException e) {
        LOG.debug("failed to convert to URI", e);
      }
      return super.retrieveResource(url);
    }
  }
}
