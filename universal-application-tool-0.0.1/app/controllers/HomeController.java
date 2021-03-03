package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import auth.UatProfile;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.pac4j.core.exception.TechnicalException;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.LoginForm;

public class HomeController extends Controller {

  private final LoginForm loginForm;
  private final ProfileUtils profileUtils;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public HomeController(
      LoginForm form, ProfileUtils profileUtils, HttpExecutionContext httpExecutionContext) {
    this.loginForm = checkNotNull(form);
    this.profileUtils = checkNotNull(profileUtils);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
  }

  public CompletionStage<Result> index(Http.Request request) {
    Optional<UatProfile> maybeProfile = profileUtils.currentUserProfile(request);

    if (maybeProfile.isEmpty()) {
      return CompletableFuture.completedFuture(
          redirect(controllers.routes.HomeController.loginForm(Optional.empty())));
    }

    UatProfile profile = maybeProfile.get();

    if (profile.isUatAdmin()) {
      return CompletableFuture.completedFuture(
          redirect(controllers.admin.routes.AdminProgramController.index()));
    } else {
      return profile
          .getApplicant()
          .thenApplyAsync(
              applicant ->
                  redirect(
                      controllers.applicant.routes.ApplicantProgramsController.index(applicant.id)),
              httpExecutionContext.current());
    }
  }

  public Result loginForm(Http.Request request, Optional<String> message)
      throws TechnicalException {
    return ok(loginForm.render(request, message));
  }
}
