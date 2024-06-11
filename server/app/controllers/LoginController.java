package controllers;

import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;
import static play.mvc.Results.redirect;

import auth.AdminAuthClient;
import auth.ApplicantAuthClient;
import auth.CiviFormHttpActionAdapter;
import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.play.PlayWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

/**
 * These routes can be hit even if you are logged in already, which is what allows the merge logic -
 * normally you need to be logged out in order to be redirected to a login page.
 */
public class LoginController extends Controller {

  private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
  private final IndirectClient adminClient;

  private final IndirectClient applicantClient;

  private final SessionStore sessionStore;

  private final HttpActionAdapter httpActionAdapter;

  private final Config config;

  @Inject
  public LoginController(
      @AdminAuthClient @Nullable IndirectClient adminClient,
      @ApplicantAuthClient @Nullable IndirectClient applicantClient,
      CiviFormHttpActionAdapter civiFormHttpActionAdapter,
      SessionStore sessionStore,
      Config config) {
    this.adminClient = adminClient;
    this.applicantClient = applicantClient;
    this.sessionStore = Preconditions.checkNotNull(sessionStore);
    this.httpActionAdapter = civiFormHttpActionAdapter;
    this.config = config;
  }

  public Result adminLogin(Http.Request request) {
    return login(request, adminClient);
  }

  public Result applicantLogin(Http.Request request, Optional<String> redirectTo) {
    if (redirectTo.isEmpty()) {
      return login(request, applicantClient);
    }
    return login(request, applicantClient)
        .addingToSession(request, REDIRECT_TO_SESSION_KEY, redirectTo.get());
  }

  public Result register(Http.Request request) {
    String registerUrl =
        config.hasPath("applicant_register_uri") ? config.getString("applicant_register_uri") : "";
    if (registerUrl.isBlank()) {
      logger.warn("Register uri is expected, but not set in the config.");
      return login(request, applicantClient);
    }
    // Redirect to the registration URL - then, when the user visits the site again, automatically
    // log them in.
    return redirect(registerUrl)
        .addingToSession(
            request,
            REDIRECT_TO_SESSION_KEY,
            routes.LoginController.applicantLogin(Optional.empty()).url());
  }

  // Logic taken from org.pac4j.play.deadbolt2.Pac4jHandler.beforeAuthCheck.
  private Result login(Http.Request request, IndirectClient client) {
    if (client == null) {
      return badRequest("Login not configured.");
    }
    PlayWebContext webContext = new PlayWebContext(request);
    if (client instanceof OidcClient) {
      webContext.setRequestAttribute(
          OidcConfiguration.SCOPE, ((OidcClient) client).getConfiguration().getScope());
    }
    try {
      Optional<RedirectionAction> redirectMaybe =
          client.getRedirectionAction(new CallContext(webContext, sessionStore));
      // If pac4j returns a redirect action, follow it.
      if (redirectMaybe.isPresent()) {
        RedirectionAction redirect = redirectMaybe.get();
        return (Result) httpActionAdapter.adapt(redirect, webContext);
      }

    } catch (final HttpAction e) {
      // But it also returns an HttpAction (redirect) exception in certain cases (such as after a
      // failed login attempt)
      // We also need to follow the return exception's redirect.
      return (Result) httpActionAdapter.adapt(e, webContext);
    }
    return badRequest("cannot redirect to identity provider");
  }
}
