package auth.oidc.applicant;

import auth.ProfileFactory;
import auth.oidc.CiviformOidcLogoutActionBuilder;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import javax.inject.Provider;
import org.pac4j.oidc.client.OidcClient;
import repository.UserRepository;

/**
 * Provider for auth0.com. Auth0 mostly implements OIDC protocol so it relies on base implementation
 * of GenericOidcProvider. But Auth0 doesn't implement logout well which is encapsulated in this
 * class.
 *
 * <p>https://auth0.com/docs/authenticate/protocols/openid-connect-protocol
 */
public class Auth0ClientProvider extends GenericOidcClientProvider {

  @Inject
  public Auth0ClientProvider(
      Config configuration,
      ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider) {
    super(configuration, profileFactory, applicantRepositoryProvider);
  }

  @Override
  protected Optional<String> getProviderName() {
    return Optional.of("Auth0");
  }

  @Override
  protected Optional<String> getLogoutURL() {
    // Auth0 doesn't set end_session_endpoint like spec suggests.
    // https://openid.net/specs/openid-connect-session-1_0-17.html#OPMetadata
    // Instead, we need to set it to /v2/logout ourselves:
    // https://auth0.com/docs/api/authentication#logout
    try {
      URL discoveryUri = new URL(getDiscoveryURI());
      return Optional.of(new URL(discoveryUri, "/v2/logout").toString());
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Unparseable discovery URI used for applicant OIDC", e);
    }
  }

  @Override
  public OidcClient get() {
    OidcClient client = super.get();

    // See https://auth0.com/docs/api/authentication#logout
    ((CiviformOidcLogoutActionBuilder) client.getLogoutActionBuilder())
        .setPostLogoutRedirectParam("returnTo");

    return client;
  }
}
