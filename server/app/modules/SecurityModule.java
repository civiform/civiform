package modules;

import static com.google.common.base.Preconditions.checkNotNull;
import static play.mvc.Results.forbidden;
import static play.mvc.Results.redirect;

import auth.AdminAuthClient;
import auth.ApiAuthenticator;
import auth.ApplicantAuthClient;
import auth.AuthIdentityProviderName;
import auth.Authorizers;
import auth.CiviFormProfileData;
import auth.FakeAdminClient;
import auth.GuestClient;
import auth.ProfileFactory;
import auth.Roles;
import auth.oidc.AdOidcProvider;
import auth.oidc.IdcsOidcProvider;
import auth.saml.LoginRadiusSamlProvider;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import controllers.routes;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import org.pac4j.core.authorization.authorizer.RequireAllRolesAuthorizer;
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.profile.BasicUserProfile;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.play.CallbackController;
import org.pac4j.play.LogoutController;
import org.pac4j.play.http.PlayHttpActionAdapter;
import org.pac4j.play.store.PlayCookieSessionStore;
import org.pac4j.play.store.ShiroAesDataEncrypter;
import play.Environment;

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

    String applicantAuthClient = "idcs";

    try {
      applicantAuthClient = configuration.getString("auth.applicant_idp");
    } catch (ConfigurationException ignore) {
      // Default to IDCS.
    }

    bindAdminIdpProvider();
    bindApplicantIdpProvider(applicantAuthClient);
  }

  private void bindAdminIdpProvider() {
    // Currently the only supported admin auth provider. As we add other admin auth providers,
    // this can be converted into a switch statement.
    bind(IndirectClient.class)
        .annotatedWith(AdminAuthClient.class)
        .toProvider(AdOidcProvider.class);
  }

  private void bindApplicantIdpProvider(String applicantIdpName) {
    AuthIdentityProviderName idpName = AuthIdentityProviderName.forString(applicantIdpName).get();

    switch (idpName) {
      case LOGIN_RADIUS_APPLICANT:
        bind(IndirectClient.class)
            .annotatedWith(ApplicantAuthClient.class)
            .toProvider(LoginRadiusSamlProvider.class);
        break;
      case IDCS_APPLICANT:
      default:
        bind(IndirectClient.class)
            .annotatedWith(ApplicantAuthClient.class)
            .toProvider(IdcsOidcProvider.class);
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

  @Provides
  @Singleton
  protected DirectBasicAuthClient apiAuthClient(ApiAuthenticator apiAuthenticator) {
    DirectBasicAuthClient client =
        new DirectBasicAuthClient(
            apiAuthenticator,
            (Credentials credentials, WebContext context, SessionStore sessionStore) -> {
              BasicUserProfile profile = new BasicUserProfile();
              String keyId = ((UsernamePasswordCredentials) credentials).getUsername();
              profile.setId(keyId);
              return Optional.of(profile);
            });
    client.setSaveProfileInSession(false);
    return client;
  }

  @Provides
  @Singleton
  protected Config provideConfig(
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
