package modules;

import static play.mvc.Results.forbidden;
import static play.mvc.Results.unauthorized;

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
import play.Environment;

public class SecurityModule extends AbstractModule {

  private final com.typesafe.config.Config configuration;
  private final String baseUrl;

  public SecurityModule(
      final Environment environment, final com.typesafe.config.Config configuration) {
    this.configuration = configuration;
    if (configuration.hasPath("baseUrl")) {
      this.baseUrl = configuration.getString("baseUrl");
    } else {
      this.baseUrl = "http://localhost:9000";
    }
  }

  @Override
  protected void configure() {
    // After logging in you are redirected to '/', and auth autorenews.
    final CallbackController callbackController = new CallbackController();
    callbackController.setDefaultUrl("/");
    callbackController.setRenewSession(true);
    bind(CallbackController.class).toInstance(callbackController);

    // you can logout by hitting the logout endpoint, you'll be redirected to root page.
    final LogoutController logoutController = new LogoutController();
    logoutController.setDefaultUrl("/");
    logoutController.setDestroySession(true);
    bind(LogoutController.class).toInstance(logoutController);

    bind(SessionStore.class).to(PlayCookieSessionStore.class);
  }

  @Provides
  @Singleton
  protected DirectBasicAuthClient provideDirectBasicAuthClient() {
    return new DirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator());
  }

  @Provides
  @Singleton
  protected FormClient provideFormClient() {
    return new FormClient(baseUrl + "/loginForm", new SimpleTestUsernamePasswordAuthenticator());
  }

  @Provides
  @Singleton
  protected Config provideConfig(
      DirectBasicAuthClient directBasicAuthClient, FormClient formClient) {
    final Clients clients = new Clients(baseUrl + "/callback");
    clients.setClients(directBasicAuthClient, formClient);
    PlayHttpActionAdapter.INSTANCE
        .getResults()
        .put(
            HttpConstants.UNAUTHORIZED,
            unauthorized("401 not authorized").as(HttpConstants.HTML_CONTENT_TYPE));
    PlayHttpActionAdapter.INSTANCE
        .getResults()
        .put(
            HttpConstants.FORBIDDEN,
            forbidden("403 forbidden").as(HttpConstants.HTML_CONTENT_TYPE));
    final Config config = new Config();
    config.setClients(clients);
    config.addAuthorizer("uatadmin", new RequireAllRolesAuthorizer("ROLE_UAT_ADMIN"));
    config.addAuthorizer("programadmin", new RequireAllRolesAuthorizer("ROLE_PROGRAM_ADMIN"));
    config.addAuthorizer("applicant", new RequireAllRolesAuthorizer("ROLE_APPLICANT"));
    config.addAuthorizer("intermediary", new RequireAllRolesAuthorizer("ROLE_TI"));

    config.setHttpActionAdapter(PlayHttpActionAdapter.INSTANCE);
    return config;
  }
}
