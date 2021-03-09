package controllers;

import java.util.Optional;
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

public class LoginController extends Controller {
  @Inject
  @Named("idcs")
  OidcClient idcsClient;

  @Inject
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

  private Result login(Http.Request request, OidcClient client) {
    PlayWebContext webContext = new PlayWebContext(request);
    Optional<RedirectionAction> redirect = client.getRedirectionAction(webContext, sessionStore);
    if (redirect.isPresent()) {
      return (Result) httpActionAdapter.adapt(redirect.get(), webContext);
    }
    return badRequest("cannot redirect to identity provider");
  }
}
