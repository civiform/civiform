package controllers;

import static play.mvc.Results.redirect;

import java.util.Optional;
import play.mvc.Http;
import play.mvc.Result;

/**
 * Class for handling legacy routes after they have been removed between release cycles. All the
 * routes in this class should eventually be deleted.
 */
public class LegacyRoutesController {

  public Result idcsLoginWithRedirect(Http.Request request, Optional<String> redirectTo) {
    return redirect(routes.LoginController.applicantLogin(redirectTo));
  }

  public Result adfsLogin(Http.Request request) {
    return redirect(routes.LoginController.adminLogin());
  }

  public Result loginRadiusLoginWithRedirect(Http.Request request, Optional<String> redirectTo) {
    return redirect(routes.LoginController.applicantLogin(redirectTo));
  }
}
