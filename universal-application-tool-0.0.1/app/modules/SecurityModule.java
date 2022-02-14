package modules;

import static com.google.common.base.Preconditions.checkNotNull;
import static play.mvc.Results.forbidden;
import static play.mvc.Results.redirect;

import auth.AdOidcClient;
import auth.AdfsProfileAdapter;
import auth.Authorizers;
import auth.CiviFormProfileData;
import auth.FakeAdminClient;
import auth.GuestClient;
import auth.IdcsOidcClient;
import auth.IdcsProfileAdapter;
import auth.LoginRadiusSamlClient;
import auth.ProfileFactory;
import auth.Roles;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import controllers.routes;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import javax.inject.Provider;
import net.minidev.json.JSONObject;
import org.pac4j.core.authorization.authorizer.RequireAllRolesAuthorizer;
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.http.callback.PathParameterCallbackUrlResolver;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.play.CallbackController;
import org.pac4j.play.LogoutController;
import org.pac4j.play.http.PlayHttpActionAdapter;
import org.pac4j.play.store.PlayCookieSessionStore;
import org.pac4j.play.store.ShiroAesDataEncrypter;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;
import play.Environment;
import repository.UserRepository;

/** SecurityModule configures and initializes all authentication and authorization classes. */
public class SecurityModule extends AbstractModule {

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
    logoutController.setDefaultUrl(routes.HomeController.index().url());
    logoutController.setDestroySession(true);
    bind(LogoutController.class).toInstance(logoutController);

    // This is a weird one.  :)  The cookie session store refuses to serialize any
    // classes it doesn't explicitly trust.  A bug in pac4j interacts badly with
    // sbt's autoreload, so we have a little workaround here.  configure() gets called on every
    // startup,
    // but the JAVA_SERIALIZER object is only initialized on initial startup.
    // So, on a second startup, we'll add the CiviFormProfileData a second time.  The
    // trusted classes set should dedupe CiviFormProfileData against the old CiviFormProfileData,
    // but it's technically a different class with the same name at that point,
    // which triggers the bug.  So, we just clear the classes, which will be empty
    // on first startup and will contain the profile on subsequent startups,
    // so that it's always safe to add the profile.
    // We will need to do this for every class we want to store in the cookie.
    PlayCookieSessionStore.JAVA_SERIALIZER.clearTrustedClasses();
    PlayCookieSessionStore.JAVA_SERIALIZER.addTrustedClass(CiviFormProfileData.class);

    // We need to use the secret key to generate the encrypter / decrypter for the
    // session store, so that cookies from version n of the application can be
    // read by version n + 1.  This is especially important for dev, otherwise
    // we're going to spend a lot of time deleting cookies.
    Random r = new Random();
    r.setSeed(this.configuration.getString("play.http.secret.key").hashCode());
    byte[] aesKey = new byte[32];
    r.nextBytes(aesKey);
    PlayCookieSessionStore sessionStore =
        new PlayCookieSessionStore(new ShiroAesDataEncrypter(aesKey));
    bind(SessionStore.class).toInstance(sessionStore);
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

  /** Creates a singleton object of OidcClient configured for IDCS and initializes it on startup. */
  @Provides
  @Nullable
  @Singleton
  @IdcsOidcClient
  protected OidcClient provideIDCSClient(
      ProfileFactory profileFactory, Provider<UserRepository> applicantRepositoryProvider) {
    if (!this.configuration.hasPath("idcs.client_id")
        || !this.configuration.hasPath("idcs.secret")) {
      return null;
    }
    OidcConfiguration config = new OidcConfiguration();
    config.setClientId(this.configuration.getString("idcs.client_id"));
    config.setSecret(this.configuration.getString("idcs.secret"));
    config.setDiscoveryURI(this.configuration.getString("idcs.discovery_uri"));
    config.setResponseMode("form_post");
    // Our local fake IDCS doesn't support 'token' auth.
    if (baseUrl.contains("localhost:")) {
      config.setResponseType("id_token");
    } else {
      config.setResponseType("id_token token");
    }
    config.setUseNonce(true);
    config.setWithState(false);
    config.setScope("openid profile email");
    OidcClient client = new OidcClient(config);
    client.setCallbackUrl(baseUrl + "/callback");
    client.setProfileCreator(
        new IdcsProfileAdapter(config, client, profileFactory, applicantRepositoryProvider));
    client.setCallbackUrlResolver(new PathParameterCallbackUrlResolver());
    client.init();
    return client;
  }

  /** Creates a singleton object of OidcClient configured for LoginRadius and initializes it on startup. */
  @Provides
  @Nullable
  @Singleton
  @LoginRadiusSamlClient
  protected SAML2Client provideLoginRadiusClient(
      ProfileFactory profileFactory, Provider<UserRepository> applicantRepositoryProvider) {
    if (!this.configuration.hasPath("login_radius.keystore_password")
        || !this.configuration.hasPath("login_radius.private_key_password")
        || !this.configuration.hasPath("login_radius.api_key")) {
      return null;
    }

    String metadataResourceUrl = String.format("%s?apikey=%s?appName=$s",
        this.configuration.getString("login_radius.metadata_url"),
        this.configuration.getString("login_radius.api_key"),
        this.configuration.getString("login_radius.saml_app_name")
    );
    SAML2Configuration config = new SAML2Configuration();
    config.setKeystoreResourceClasspath(this.configuration.getString("login_radius.keystore_name"));
    config.setKeystorePassword(this.configuration.getString("login_radius.keystore_password"));
    config.setPrivateKeyPassword(this.configuration.getString("login_radius.private_key_password"));
    config.setIdentityProviderMetadataResourceUrl(metadataResourceUrl);
    config.setServiceProviderEntityId(baseUrl + "/callback");
    
    SAML2Client client = new SAML2Client(config);

    // TODO loginradiusprofileadapter

    client.setCallbackUrlResolver(new PathParameterCallbackUrlResolver());
    client.init();
    return client;
  }


  /** Creates a singleton object of OidcClient configured for AD and initializes it on startup. */
  @Provides
  @Nullable
  @Singleton
  @AdOidcClient
  protected OidcClient provideAdClient(
      ProfileFactory profileFactory, Provider<UserRepository> applicantRepositoryProvider) {
    if (!this.configuration.hasPath("adfs.client_id")
        || !this.configuration.hasPath("adfs.secret")) {
      return null;
    }
    OidcConfiguration config = new OidcConfiguration();
    config.setClientId(this.configuration.getString("adfs.client_id"));
    config.setSecret(this.configuration.getString("adfs.secret"));
    config.setDiscoveryURI(this.configuration.getString("adfs.discovery_uri"));
    config.setResponseMode("form_post");
    config.setResponseType("id_token");
    config.setScope("openid profile email allatclaims");
    config.setUseNonce(true);
    config.setWithState(false);
    OidcClient client = new OidcClient(config);
    client.setName("AdClient");
    client.setCallbackUrl(baseUrl + "/callback");
    client.setProfileCreator(
        new AdfsProfileAdapter(
            config, client, profileFactory, this.configuration, applicantRepositoryProvider));
    client.setCallbackUrlResolver(new PathParameterCallbackUrlResolver());
    client.init();
    return client;
  }

  @Provides
  @Singleton
  protected Config provideConfig(
      GuestClient guestClient,
      @AdOidcClient @Nullable OidcClient adClient,
      @IdcsOidcClient @Nullable OidcClient idcsClient,
      FakeAdminClient fakeAdminClient) {
    List<Client> clientList = new ArrayList<>();
    clientList.add(guestClient);
    if (idcsClient != null) {
      clientList.add(idcsClient);
    }
    if (adClient != null) {
      clientList.add(adClient);
    }
    if (fakeAdminClient.canEnable(URI.create(baseUrl).getHost())) {
      clientList.add(fakeAdminClient);
    }
    Clients clients = new Clients(baseUrl + "/callback");
    clients.setClients(clientList);
    PlayHttpActionAdapter.INSTANCE
        .getResults()
        .putAll(
            ImmutableMap.of(
                HttpConstants.UNAUTHORIZED,
                redirect(routes.HomeController.loginForm(Optional.of("login"))),
                HttpConstants.FORBIDDEN,
                forbidden("403 forbidden").as(HttpConstants.HTML_CONTENT_TYPE)));
    Config config = new Config();
    config.setClients(clients);
    config.addAuthorizer(
        Authorizers.PROGRAM_ADMIN.toString(),
        new RequireAllRolesAuthorizer(Roles.ROLE_PROGRAM_ADMIN.toString()));
    config.addAuthorizer(
        Authorizers.CIVIFORM_ADMIN.toString(),
        new RequireAllRolesAuthorizer(Roles.ROLE_CIVIFORM_ADMIN.toString()));
    config.addAuthorizer(
        Authorizers.APPLICANT.toString(),
        new RequireAllRolesAuthorizer(Roles.ROLE_APPLICANT.toString()));
    config.addAuthorizer(
        Authorizers.TI.toString(), new RequireAllRolesAuthorizer(Roles.ROLE_TI.toString()));
    config.addAuthorizer(
        Authorizers.ANY_ADMIN.toString(),
        new RequireAnyRoleAuthorizer(
            Roles.ROLE_CIVIFORM_ADMIN.toString(), Roles.ROLE_PROGRAM_ADMIN.toString()));

    config.setHttpActionAdapter(PlayHttpActionAdapter.INSTANCE);
    return config;
  }
}
