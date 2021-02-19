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
import java.util.Random;
import org.pac4j.core.authorization.authorizer.RequireAllRolesAuthorizer;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.play.CallbackController;
import org.pac4j.play.LogoutController;
import org.pac4j.play.http.PlayHttpActionAdapter;
import org.pac4j.play.store.PlayCookieSessionStore;
import org.pac4j.play.store.ShiroAesDataEncrypter;
import play.Environment;

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

    // This is a weird one.  :)  The cookie session store refuses to serialize any
    // classes it doesn't explicitly trust.  A bug in pac4j interacts badly with
    // sbt's autoreload, so we have a little workaround here.  configure() gets called on every
    // startup,
    // but the JAVA_SERIALIZER object is only initialized on initial startup.
    // So, on a second startup, we'll add the UATProfile a second time.  The
    // trusted classes set should dedupe UATProfile against the old UATProfile,
    // but it's technically a different class with the same name at that point,
    // which triggers the bug.  So, we just clear the classes, which will be empty
    // on first startup and will contain the profile on subsequent startups,
    // so that it's always safe to add the profile.
    // We will need to do this for every class we want to store in the cookie.
    PlayCookieSessionStore.JAVA_SERIALIZER.clearTrustedClasses();
    PlayCookieSessionStore.JAVA_SERIALIZER.addTrustedClass(UATProfile.class);

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
