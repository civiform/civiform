package controllers;

import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.oidc.client.OidcClient;
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
  @Inject
  @Nullable
  @Named("idcs")
  OidcClient idcsClient;

  @Inject
  @Nullable
  @Named("ad")
  OidcClient adClient;

  @Inject SessionStore sessionStore;

  HttpActionAdapter httpActionAdapter = PlayHttpActionAdapter.INSTANCE;

  public Result idcsLogin(Http.Request request) {
    return login(request, idcsClient);
  }

  public Result adfsLogin(Http.Request request) {
    return login(request, adClient);
  }

  // Logic taken from org.pac4j.play.deadbolt2.Pac4jHandler.beforeAuthCheck.
  private Result login(Http.Request request, OidcClient client) {
    PlayWebContext webContext = new PlayWebContext(request);
    Optional<RedirectionAction> redirect = client.getRedirectionAction(webContext, sessionStore);
    if (redirect.isPresent()) {
      return (Result) httpActionAdapter.adapt(redirect.get(), webContext);
    }
    return badRequest("cannot redirect to identity provider");
  }
}
