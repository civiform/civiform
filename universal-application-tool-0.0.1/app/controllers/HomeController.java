package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import auth.UatProfile;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.play.java.Secure;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.LoginForm;
import views.html.index;

public class HomeController extends Controller {

  private final AssetsFinder assetsFinder;
  private final LoginForm loginForm;
  private final ProfileUtils profileUtils;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public HomeController(
      AssetsFinder assetsFinder,
      LoginForm form,
      ProfileUtils profileUtils,
      HttpExecutionContext httpExecutionContext) {
    this.assetsFinder = checkNotNull(assetsFinder);
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

  public Result playIndex() {
    return ok(index.render("public index", assetsFinder));
  }

  @Secure
  public Result securePlayIndex() {
    return ok(index.render("You are logged in.", assetsFinder));
  }
}
