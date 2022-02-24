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
import auth.ProfileFactory;
import auth.Roles;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import controllers.routes;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import javax.inject.Provider;
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
    // this is the resource identifier that tells adfs that this is civiform
    config.setClientId(this.configuration.getString("adfs.client_id"));

    // this is the token that we create within adfs
    config.setSecret(this.configuration.getString("adfs.secret"));

    // oidc convention that auth provider has an endpoint that can get text blob
    // that gets back public key/etc (can just curl this if curious,
    // get from endpoints in azure)
    config.setDiscoveryURI(this.configuration.getString("adfs.discovery_uri"));

    // tells AD to use a post response when it sends info back from
    // the auth request
    config.setResponseMode("form_post");

    // we want adfs to give us an id token back from this request
    config.setResponseType("id_token");

    // scopes are the other things that we want from the adfs endpoint
    // (needs to also be configured on adfs).
    // note azure ad has the extra claim: allatclaims which returns
    // access token in the id_token. might be used for
    // the group that tells who are in what group.
    String[] defaultScopes = {"openid", "profile", "email"};
    String[] extraScopes = this.configuration.getString("adfs.additional_scopes").split(" ");
    ArrayList<String> allClaims = new ArrayList<>();
    Collections.addAll(allClaims, defaultScopes);
    Collections.addAll(allClaims, extraScopes);
    config.setScope(String.join(" ", allClaims));

    // security setting that adds a random number to ensure cannot be reused
    config.setUseNonce(true);

    // ? relationship with adfs is not stateful ?
    config.setWithState(false);

    OidcClient client = new OidcClient(config);
    client.setName("AdClient");

    // telling adfs where to send people where to go back to
    client.setCallbackUrl(baseUrl + "/callback");

    // this is specific to the implemention using pac4j. has concept of a profile
    // for different identity profiles we have different creators. this is
    // what links the user to the stuff they have access to within the application
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
