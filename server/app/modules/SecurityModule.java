package modules;

import static com.google.common.base.Preconditions.checkNotNull;
import static play.mvc.Results.forbidden;
import static play.mvc.Results.redirect;

import auth.AdminAuthClient;
import auth.ApiAuthenticator;
import auth.ApplicantAuthClient;
import auth.AuthIdentityProviderName;
import auth.Authorizers;
import auth.CiviFormHttpActionAdapter;
import auth.CiviFormSessionStoreFactory;
import auth.FakeAdminClient;
import auth.GuestClient;
import auth.ProfileFactory;
import auth.Role;
import auth.oidc.admin.AdfsClientProvider;
import auth.oidc.applicant.Auth0ClientProvider;
import auth.oidc.applicant.GenericOidcClientProvider;
import auth.oidc.applicant.IdcsClientProvider;
import auth.oidc.applicant.LoginGovClientProvider;
import auth.saml.LoginRadiusClientProvider;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.util.Providers;
import controllers.routes;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.authorization.authorizer.RequireAllRolesAuthorizer;
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.profile.BasicUserProfile;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.play.CallbackController;
import org.pac4j.play.LogoutController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;

/** SecurityModule configures and initializes all authentication and authorization classes. */
public class SecurityModule extends AbstractModule {

  private static final Logger logger = LoggerFactory.getLogger(SecurityModule.class);
  private final com.typesafe.config.Config configuration;
  private final String baseUrl;

  public SecurityModule(Environment environment, com.typesafe.config.Config configuration) {
    checkNotNull(environment);
    this.configuration = checkNotNull(configuration);
    this.baseUrl = configuration.getString("base_url");
  }

  @Override
  protected void configure() {
    // After logging in you are redirected to '/', and auth autorenews.
    CallbackController callbackController = new CallbackController();
    callbackController.setDefaultUrl(routes.HomeController.index().url());
    callbackController.setRenewSession(true);
    bind(CallbackController.class).toInstance(callbackController);

    // you can logout by hitting the logout endpoint, you'll be redirected to root page.
    LogoutController logoutController = new LogoutController();
    logoutController.setDefaultUrl(baseUrl + routes.HomeController.index().url());
    logoutController.setLocalLogout(true);
    logoutController.setDestroySession(true);

    Boolean shouldPerformAuthProviderLogout = configuration.getBoolean("auth.oidc_provider_logout");
    logoutController.setCentralLogout(shouldPerformAuthProviderLogout);
    bind(LogoutController.class).toInstance(logoutController);

    CiviFormSessionStoreFactory civiFormSessionStoreFactory =
        new CiviFormSessionStoreFactory(this.configuration);

    bind(SessionStore.class).toInstance(civiFormSessionStoreFactory.newSessionStore());
    bind(CiviFormSessionStoreFactory.class).toInstance(civiFormSessionStoreFactory);

    bindAdminIdpProvider(configuration);
    bindApplicantIdpProvider(configuration);
  }

  private void bindAdminIdpProvider(com.typesafe.config.Config config) {
    AuthIdentityProviderName idpName =
        AuthIdentityProviderName.adminIdentityProviderfromConfig(config);

    switch (idpName) {
      case ADFS_ADMIN:
        bind(IndirectClient.class)
            .annotatedWith(AdminAuthClient.class)
            .toProvider(AdfsClientProvider.class);
        logger.info("Using ADFS for admin auth provider");
        break;
      case GENERIC_OIDC_ADMIN:
        bind(IndirectClient.class)
            .annotatedWith(AdminAuthClient.class)
            .toProvider(auth.oidc.admin.GenericOidcClientProvider.class);
        logger.info("Using Generic OIDC for admin auth provider");
        break;
      default:
        throw new ConfigurationException("Unable to create admin identity provider: " + idpName);
    }
  }

  private void bindApplicantIdpProvider(com.typesafe.config.Config config) {
    AuthIdentityProviderName idpName =
        AuthIdentityProviderName.applicantIdentityProviderfromConfig(config);

    switch (idpName) {
      case DISABLED_APPLICANT:
        bind(IndirectClient.class)
            .annotatedWith(ApplicantAuthClient.class)
            .toProvider(Providers.of(null));
        logger.info("No applicant auth provider");
        break;
      case LOGIN_RADIUS_APPLICANT:
        bind(IndirectClient.class)
            .annotatedWith(ApplicantAuthClient.class)
            .toProvider(LoginRadiusClientProvider.class);
        logger.info("Using Login Radius for applicant auth provider");
        break;
      case IDCS_APPLICANT:
        bind(IndirectClient.class)
            .annotatedWith(ApplicantAuthClient.class)
            .toProvider(IdcsClientProvider.class);
        logger.info("Using IDCS for applicant auth provider");
        break;
      case GENERIC_OIDC_APPLICANT:
        bind(IndirectClient.class)
            .annotatedWith(ApplicantAuthClient.class)
            .toProvider(GenericOidcClientProvider.class);
        logger.info("Using generic OIDC for applicant auth provider");
        break;
      case LOGIN_GOV_APPLICANT:
        bind(IndirectClient.class)
            .annotatedWith(ApplicantAuthClient.class)
            .toProvider(LoginGovClientProvider.class);
        logger.info("Using login.gov PKCE OIDC for applicant auth provider");
        break;
      case AUTH0_APPLICANT:
        bind(IndirectClient.class)
            .annotatedWith(ApplicantAuthClient.class)
            .toProvider(Auth0ClientProvider.class);
        logger.info("Using Auth0 for applicant auth provider");
        break;
      default:
        logger.info("No provider specified for for applicants");
    }
  }

  @Provides
  @Singleton
  protected GuestClient guestClient(ProfileFactory profileFactory) {
    return new GuestClient(profileFactory);
  }

  @Provides
  @Singleton
  protected FakeAdminClient fakeAdminClient(ProfileFactory profileFactory) {
    return new FakeAdminClient(profileFactory, this.configuration);
  }

  // CiviForm uses HTTP basic auth for authenticating API calls. The username in the basic auth
  // credentials is the API key ID, and the password is the API key secret.
  @Provides
  @Singleton
  protected DirectBasicAuthClient apiAuthClient(ApiAuthenticator apiAuthenticator) {
    DirectBasicAuthClient client =
        new DirectBasicAuthClient(
            apiAuthenticator,
            // This callback is used to create the API user's pac4j profile after they have
            // successfully authenticated. In practice, that profile is just an object we
            // use to store the API key ID so that it can be used to look up the
            // authenticated caller's ApiKey in controller code.
            (CallContext callContext, Credentials credentials) -> {
              BasicUserProfile profile = new BasicUserProfile();
              String keyId = ((UsernamePasswordCredentials) credentials).getUsername();
              profile.setId(keyId);
              return Optional.of(profile);
            });

    // The API does not support cookies, so here we tell pac4j not to set a cookie with the API
    // user's profile.
    client.setSaveProfileInSession(false);
    return client;
  }

  // The action adapter allows pac4j-consuming code to intervene and change the default behavior of
  // handling a given HTTP result. For example, redirecting to the homepage when a user is
  // unauthorized instead of showing them an error page.
  @Provides
  @Singleton
  protected CiviFormHttpActionAdapter provideCiviFormHttpActionAdapter() {
    var actionAdapter = new CiviFormHttpActionAdapter();

    actionAdapter
        .getResults()
        .putAll(
            ImmutableMap.of(
                // Redirect unauthorized requests to the home page. This behavior is bypassed
                // for API requests in CiviFormHttpActionAdapter.
                HttpConstants.UNAUTHORIZED,
                redirect(routes.HomeController.index()),

                // Display the string "403 forbidden" to forbidden requests.
                // Helpful for test assertions.
                HttpConstants.FORBIDDEN,
                forbidden("403 forbidden").as(HttpConstants.HTML_CONTENT_TYPE)));

    return actionAdapter;
  }

  // A 'client' in the context of pac4j is an authentication mechanism.
  // Docs: https://www.pac4j.org/docs/clients.html
  @Provides
  @Singleton
  protected Clients provideClients(
      GuestClient guestClient,
      @ApplicantAuthClient @Nullable IndirectClient applicantAuthClient,
      @AdminAuthClient @Nullable IndirectClient adminAuthClient,
      FakeAdminClient fakeAdminClient,
      DirectBasicAuthClient apiAuthClient) {
    List<Client> clientList = new ArrayList<>();

    clientList.add(guestClient);
    clientList.add(apiAuthClient);

    if (applicantAuthClient != null) {
      clientList.add(applicantAuthClient);
    }
    if (adminAuthClient != null) {
      clientList.add(adminAuthClient);
    }
    if (fakeAdminClient.canEnable(URI.create(baseUrl).getHost())) {
      clientList.add(fakeAdminClient);
    }

    Clients clients = new Clients(baseUrl + "/callback");
    clients.setClients(clientList);

    return clients;
  }

  // Authorizers in pac4j have a string key which can be specified in the @SecureAction annotation
  // on a controller method to indicate that a given authorizer should be used to guard access.
  // CiviForm's authorizers are mostly role based, where the authorizer checks which role a
  // profile has to determine if it is allowed access. More granular permissions, such as whether
  // a particular applicant is permitted to view a particular application, are done in the
  // controller method implementation.
  // Docs: https://www.pac4j.org/docs/authorizers.html
  @Provides
  @Singleton
  protected ImmutableMap<String, Authorizer> provideAuthorizers() {
    return ImmutableMap.of(
        // Having either ROLE_CIVIFORM_ADMIN or ROLE_PROGRAM_ADMIN authorizes a profile as
        // ANY_ADMIN.
        Authorizers.ANY_ADMIN.toString(),
        new RequireAnyRoleAuthorizer(
            Role.ROLE_CIVIFORM_ADMIN.toString(), Role.ROLE_PROGRAM_ADMIN.toString()),

        // Having ROLE_PROGRAM_ADMIN authorizes a profile as PROGRAM_ADMIN.
        Authorizers.PROGRAM_ADMIN.toString(),
        new RequireAllRolesAuthorizer(Role.ROLE_PROGRAM_ADMIN.toString()),

        // Having ROLE_CIVIFORM_ADMIN authorizes a profile as CIVIFORM_ADMIN.
        Authorizers.CIVIFORM_ADMIN.toString(),
        new RequireAllRolesAuthorizer(Role.ROLE_CIVIFORM_ADMIN.toString()),

        // Having ROLE_APPLICANT authorizes a profile as APPLICANT.
        Authorizers.APPLICANT.toString(),
        new RequireAllRolesAuthorizer(Role.ROLE_APPLICANT.toString()),

        // Having ROLE_TI authorizes a profile as TI.
        Authorizers.TI.toString(),
        new RequireAllRolesAuthorizer(Role.ROLE_TI.toString()));
  }

  // This provider is consumed by play-pac4j to get the app's security configuration.
  // Docs: https://www.pac4j.org/docs/config.html
  @Provides
  @Singleton
  protected Config provideConfig(
      Clients clients,
      ImmutableMap<String, Authorizer> authorizors,
      CiviFormHttpActionAdapter civiFormHttpActionAdapter,
      CiviFormSessionStoreFactory civiFormSessionStoreFactory) {
    Config config = new Config();
    config.setClients(clients);
    config.setAuthorizers(authorizors);
    config.setHttpActionAdapter(civiFormHttpActionAdapter);
    config.setSessionStoreFactory(civiFormSessionStoreFactory);
    return config;
  }
}
