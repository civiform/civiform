package controllers;

import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

/**
 * This is a convenience class to wrap org.pac4j.play.CallbackController. Controllers outside our
 * package don't seem to get registered in the reverse router, and we want to be able to refer to
 * our callbacks there. We also want programmatic access to the URLs which include client_name
 * parameters.
 */
public class CallbackController extends Controller {
  @Inject private org.pac4j.play.CallbackController wrappedController;

  public CompletionStage<Result> callback(Http.Request request, String clientName) {
    return wrappedController.callback(request);
  }
}
