package controllers.dev;

import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.export.JsonPrettifier;

/** Controller to display session data in browser. */
public final class SessionDisplayController extends Controller {
  /** Returns a text/plain page containing the session data. */
  public Result index(Http.Request request) {
    String sessionContent = JsonPrettifier.asPrettyJsonString(request.session().data());
    return ok(sessionContent).as(Http.MimeTypes.TEXT);
  }
}
