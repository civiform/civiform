package auth.oidc.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileFactory;
import auth.oidc.OidcClientProviderParams;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.typesafe.config.Config;
import java.util.ArrayList;
import java.util.Collections;
import org.pac4j.core.http.callback.PathParameterCallbackUrlResolver;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import repository.AccountRepository;

/** Provider class for the AD OIDC Client. */
public class AdfsClientProvider implements Provider<OidcClient> {

  private final Config configuration;
  private final String baseUrl;
  private final ProfileFactory profileFactory;
  private final Provider<AccountRepository> accountRepositoryProvider;

  @Inject
  public AdfsClientProvider(
      Config configuration,
      ProfileFactory profileFactory,
      Provider<AccountRepository> accountRepositoryProvider) {
    this.configuration = checkNotNull(configuration);
    this.baseUrl = configuration.getString("base_url");
    this.profileFactory = checkNotNull(profileFactory);
    this.accountRepositoryProvider = checkNotNull(accountRepositoryProvider);
  }

  @Override
  public OidcClient get() {
    if (!configuration.hasPath("adfs.client_id") || !configuration.hasPath("adfs.secret")) {
      return null;
    }
    OidcConfiguration config = new OidcConfiguration();
    // Resource identifier that tells AD that this is civiform from the portal.
    config.setClientId(configuration.getString("adfs.client_id"));

    // The token that we created within AD and use to sign our requests.
    config.setSecret(configuration.getString("adfs.secret"));

    // Endpoint that app can use to get the public keys from.
    config.setDiscoveryURI(configuration.getString("adfs.discovery_uri"));

    // Tells AD to use a post response when it sends info back from
    // the auth request.
    config.setResponseMode("form_post");

    // Tells AD to give us an id token back from this request.
    config.setResponseType("id_token");

    // Scopes are the other things that we want from the AD endpoint
    // (needs to also be configured on AD side).
    // Note: ADFS has the extra claim: allatclaims which returns
    // access token in the id_token.
    String[] defaultScopes = {"openid", "profile", "email"};
    String[] extraScopes = configuration.getString("adfs.additional_scopes").split(" ");
    ArrayList<String> allClaims = new ArrayList<>();
    Collections.addAll(allClaims, defaultScopes);
    Collections.addAll(allClaims, extraScopes);
    config.setScope(String.join(" ", allClaims));

    // Security setting that adds a random number to ensure cannot be reused.
    config.setUseNonce(true);

    // Don't have custom state data.
    config.setWithState(false);

    OidcClient client = new OidcClient(config);
    client.setName("AdClient");

    // Telling AD where to send people back to. This gets
    // combined with the name to create the url.
    client.setCallbackUrl(baseUrl + "/callback");

    // This is specific to the implementation using pac4j. pac4j has concept
    // of a profile for different identity profiles we have different creators.
    // This is what links the user to the stuff they have access to.
    client.setProfileCreator(
        new AdfsProfileCreator(
            config,
            client,
            OidcClientProviderParams.create(
                configuration, profileFactory, accountRepositoryProvider)));

    client.setCallbackUrlResolver(new PathParameterCallbackUrlResolver());
    client.init();
    return client;
  }
}
