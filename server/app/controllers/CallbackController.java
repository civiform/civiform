package controllers;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.UrlUtils;

/**
 * This is a convenience class to wrap org.pac4j.play.CallbackController. Controllers outside our
 * package don't seem to get registered in the reverse router, and we want to be able to refer to
 * our callbacks there. We also want programmatic access to the URLs which include client_name
 * parameters.
 */
public class CallbackController extends Controller {
  @Inject private org.pac4j.play.CallbackController wrappedController;

  public static final String REDIRECT_TO_SESSION_KEY = "redirectTo";

  public CompletionStage<Result> callback(Http.Request request, String clientName) {
    return wrappedController
        .callback(request)
        .thenApplyAsync(
            result -> {
              Optional<String> redirectTo = request.session().get(REDIRECT_TO_SESSION_KEY);
              if (redirectTo.isPresent()) {
                Result redirect = redirect(UrlUtils.checkIsRelativeUrl(redirectTo.get()));
                if (result.session() != null) {
                  redirect = redirect.withSession(result.session());
                  redirect = redirect.removingFromSession(request, REDIRECT_TO_SESSION_KEY);
                }
                if (result.flash() != null) {
                  redirect = redirect.withFlash(result.flash());
                }
                for (Http.Cookie cookie : result.cookies()) {
                  redirect.withCookies(cookie);
                }
                return redirect;
              }
              return result;
            });
  }

  public CompletionStage<Result> fakeAdmin(
      Http.Request request, String clientName, String adminType) {
    return wrappedController.callback(request);
  }
}
