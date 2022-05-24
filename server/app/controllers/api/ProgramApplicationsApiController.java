package controllers.api;

import auth.ProfileUtils;
import com.google.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import play.mvc.Http;
import play.mvc.Result;

/** API controller for admin access to a specific program's applications. */
public final class ProgramApplicationsApiController extends CiviFormApiController {

  @Inject
  public ProgramApplicationsApiController(ProfileUtils profileUtils) {
    super(profileUtils);
  }

  // Introduced to test API authentication logic. Implementation is next. Did not
  // want to do both in a larger pull request.
  // TODO(https://github.com/seattle-uat/civiform/issues/1744): implement
  public CompletionStage<Result> list(Http.Request request, String programSlug) {
    assertHasProgramReadPermission(request, programSlug);

    return CompletableFuture.completedFuture(ok());
  }
}
