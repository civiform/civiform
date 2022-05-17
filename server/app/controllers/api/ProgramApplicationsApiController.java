package controllers.api;

import controllers.CiviFormController;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import play.mvc.Http;
import play.mvc.Result;

/** API controller for admin access to a specific program's applications. */
public final class ProgramApplicationsApiController extends CiviFormController {

  // Introduced to test API authentication logic. Implementation is next. Did not
  // want to do both in a larger pull request.
  public CompletionStage<Result> list(Http.Request request, String programSlug) {
    return CompletableFuture.completedFuture(ok());
  }
}
