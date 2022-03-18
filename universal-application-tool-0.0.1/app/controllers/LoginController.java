package controllers;

import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;

import auth.AdminAuthClient;
import auth.ApplicantAuthClient;
import auth.AuthIdentityProviderName;
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
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

/**
 * These routes can be hit even if you are logged in already, which is what allows the merge logic -
 * normally you need to be logged out in order to be redirected to a login page.
 */
public class LoginController extends Controller {

  private final IndirectClient adminClient;

  private final IndirectClient applicantClient;

  private final SessionStore sessionStore;

  private final HttpActionAdapter httpActionAdapter;

  private final Config config;

  @Inject
  public LoginController(
      @AdminAuthClient @Nullable IndirectClient adminClient,
      @ApplicantAuthClient @Nullable IndirectClient applicantClient,
      SessionStore sessionStore,
      Config config) {
    this.adminClient = adminClient;
    this.applicantClient = applicantClient;
    this.sessionStore = Preconditions.checkNotNull(sessionStore);
    this.httpActionAdapter = PlayHttpActionAdapter.INSTANCE;
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
    String idp;
    try {
      idp = config.getString("auth.applicant_idp");
    } catch (ConfigException.Missing e) {
      // Default to IDCS.
      idp = AuthIdentityProviderName.IDCS_APPLICANT.toString();
    }

    boolean isIDCS = idp.equals(AuthIdentityProviderName.IDCS_APPLICANT.toString());

    // Because this is only being called when we know IDCS is available, this route should
    // technically
    // never happen.
    if (!isIDCS) {
      return login(request, applicantClient);
    }

    return idcsRegister(request);
  }

  // IDCS has specific register behavior that is different from other IDPs, which have the register
  // option on the same screen as the login page.
  private Result idcsRegister(Http.Request request) {
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
            routes.LoginController.applicantLogin(Optional.empty()).url());
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
