package controllers;

import static com.google.common.base.Preconditions.checkNotNull;
import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;

import auth.AdOidcClient;
import auth.AuthIdentityProviderName;
import auth.IdcsOidcClient;
import auth.LoginRadiusSamlClient;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.play.PlayWebContext;
import org.pac4j.play.http.PlayHttpActionAdapter;
import org.pac4j.saml.client.SAML2Client;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

/**
 * These routes can be hit even if you are logged in already, which is what allows the merge logic -
 * normally you need to be logged out in order to be redirected to a login page.
 */
public class LoginController extends Controller {

  private final OidcClient idcsClient;

  private final OidcClient adClient;

  private final SAML2Client loginRadiusClient;

  private final SessionStore sessionStore;

  private final HttpActionAdapter httpActionAdapter;

  private final Config config;

  private final String applicantIdp;

  @Inject
  public LoginController(
      @AdOidcClient @Nullable OidcClient adClient,
      @IdcsOidcClient @Nullable OidcClient idcsClient,
      @LoginRadiusSamlClient @Nullable SAML2Client loginRadiusClient,
      SessionStore sessionStore,
      Config config) {
    this.idcsClient = idcsClient;
    this.adClient = adClient;
    this.loginRadiusClient = loginRadiusClient;
    this.sessionStore = Preconditions.checkNotNull(sessionStore);
    this.applicantIdp = checkNotNull(config).getString("auth.applicant_idp");
    this.httpActionAdapter = PlayHttpActionAdapter.INSTANCE;
    this.config = config;
  }

  public Result loginWithRedirect(Http.Request request, Optional<String> redirectTo) {
    if (applicantIdp.equals(AuthIdentityProviderName.LOGIN_RADIUS_APPLICANT.getString())) {
      return loginRadiusLoginWithRedirect(request, redirectTo);
    } else {
      return idcsLoginWithRedirect(request, redirectTo);
    }
  }

  public Result idcsLogin(Http.Request request) {
    return login(request, idcsClient);
  }

  public Result idcsLoginWithRedirect(Http.Request request, Optional<String> redirectTo) {
    if (redirectTo.isEmpty()) {
      return idcsLogin(request);
    }
    return login(request, idcsClient)
        .addingToSession(request, REDIRECT_TO_SESSION_KEY, redirectTo.get());
  }

  public Result loginRadiusLogin(Http.Request request) {
    return login(request, loginRadiusClient);
  }

  public Result loginRadiusLoginWithRedirect(Http.Request request, Optional<String> redirectTo) {
    if (redirectTo.isEmpty()) {
      return loginRadiusLogin(request);
    }
    return login(request, loginRadiusClient)
        .addingToSession(request, REDIRECT_TO_SESSION_KEY, redirectTo.get());
  }

  public Result register(Http.Request request) {
    String registerUrl = null;
    try {
      registerUrl = config.getString("idcs.register_uri");
    } catch (ConfigException.Missing e) {
      // leave it as null / empty.
    }
    if (Strings.isNullOrEmpty(registerUrl)) {
      return badRequest("Registration is not enabled.");
    }
    // Redirect to the registration URL - then, when the user visits the site again, automatically
    // log them in.
    return redirect(registerUrl)
        .addingToSession(
            request,
            REDIRECT_TO_SESSION_KEY,
            routes.LoginController.idcsLoginWithRedirect(Optional.empty()).url());
  }

  public Result adfsLogin(Http.Request request) {
    return login(request, adClient);
  }

  // Logic taken from org.pac4j.play.deadbolt2.Pac4jHandler.beforeAuthCheck.
  private Result login(Http.Request request, IndirectClient client) {
    if (client == null) {
      return badRequest("Identity provider secrets not configured.");
    }
    PlayWebContext webContext = new PlayWebContext(request);
    if (client instanceof OidcClient) {
      webContext.setRequestAttribute(
          OidcConfiguration.SCOPE, ((OidcClient) client).getConfiguration().getScope());
    }
    Optional<RedirectionAction> redirect = client.getRedirectionAction(webContext, sessionStore);
    if (redirect.isPresent()) {
      return (Result) httpActionAdapter.adapt(redirect.get(), webContext);
    }
    return badRequest("cannot redirect to identity provider");
  }
}
