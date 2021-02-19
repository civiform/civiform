package modules;

import static com.google.common.base.Preconditions.checkNotNull;
import static play.mvc.Results.*;

import auth.GuestClient;
import auth.ProfileFactory;
import auth.UATProfile;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.pac4j.core.authorization.authorizer.RequireAllRolesAuthorizer;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.play.CallbackController;
import org.pac4j.play.LogoutController;
import org.pac4j.play.http.PlayHttpActionAdapter;
import org.pac4j.play.store.PlayCookieSessionStore;
import org.pac4j.play.store.ShiroAesDataEncrypter;
import play.Environment;
import play.api.Play;

public class SecurityModule extends AbstractModule {

  private final com.typesafe.config.Config configuration;
  private final String baseUrl;
  private static final String DEV_BASE_URL = "http://localhost:9000";

  public SecurityModule(Environment environment, com.typesafe.config.Config configuration) {
    checkNotNull(environment);
    this.configuration = checkNotNull(configuration);
    if (configuration.hasPath("baseUrl")) {
      this.baseUrl = configuration.getString("baseUrl");
    } else {
      this.baseUrl = DEV_BASE_URL;
    }
  }

  @Override
  protected void configure() {
    // After logging in you are redirected to '/', and auth autorenews.
    CallbackController callbackController = new CallbackController();
    callbackController.setDefaultUrl("/");
    callbackController.setRenewSession(true);
    bind(CallbackController.class).toInstance(callbackController);

    // you can logout by hitting the logout endpoint, you'll be redirected to root page.
    LogoutController logoutController = new LogoutController();
    logoutController.setDefaultUrl("/");
    logoutController.setDestroySession(true);
    bind(LogoutController.class).toInstance(logoutController);

    // add a good comment here about this dumb garbage
    PlayCookieSessionStore.JAVA_SERIALIZER.clearTrustedClasses();
    PlayCookieSessionStore.JAVA_SERIALIZER.addTrustedClass(UATProfile.class);
    PlayCookieSessionStore sessionStore =
        new PlayCookieSessionStore(
            new ShiroAesDataEncrypter("devkeydevkeydevkeydevkeydevkeyde".getBytes()));
    bind(SessionStore.class).toInstance(sessionStore);
  }

  @Provides
  @Singleton
  protected GuestClient guestClient(ProfileFactory profileFactory) {
    return new GuestClient(profileFactory);
  }

  @Provides
  @Singleton
  protected FormClient provideFormClient() {
    // This must match the line in `routes`.
    return new FormClient(baseUrl + "/loginForm", new SimpleTestUsernamePasswordAuthenticator());
  }

  @Provides
  @Singleton
  protected ProfileFactory provideProfileFactory() {
    return new ProfileFactory();
  }

  @Provides
  @Singleton
  protected Config provideConfig(GuestClient guestClient, FormClient formClient) {
    // This must match the line in `routes` also.
    Clients clients = new Clients(baseUrl + "/callback");
    clients.setClients(guestClient, formClient);
    PlayHttpActionAdapter.INSTANCE
        .getResults()
        .putAll(
            ImmutableMap.of(
                HttpConstants.UNAUTHORIZED,
                redirect("/loginForm?message=login"),
                HttpConstants.FORBIDDEN,
                forbidden("403 forbidden").as(HttpConstants.HTML_CONTENT_TYPE)));
    Config config = new Config();
    config.setClients(clients);
    config.addAuthorizer("uatadmin", new RequireAllRolesAuthorizer("ROLE_UAT_ADMIN"));
    config.addAuthorizer("programadmin", new RequireAllRolesAuthorizer("ROLE_PROGRAM_ADMIN"));
    config.addAuthorizer("applicant", new RequireAllRolesAuthorizer("ROLE_APPLICANT"));
    config.addAuthorizer("intermediary", new RequireAllRolesAuthorizer("ROLE_TI"));

    config.setHttpActionAdapter(PlayHttpActionAdapter.INSTANCE);
    return config;
  }
}
