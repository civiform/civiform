package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.ProfileView;

/** Controller for handling methods for user profile pages. */
public class ProfileController extends Controller {

  private final ProfileView profileView;
  private final ProfileUtils profileUtils;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public ProfileController(
      ProfileUtils profileUtils,
      ProfileView profileView,
      HttpExecutionContext httpExecutionContext) {
    this.profileUtils = checkNotNull(profileUtils);
    this.profileView = checkNotNull(profileView);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
  }

  public CompletionStage<Result> myProfile(Http.Request request) {
    Optional<CiviFormProfile> maybeProfile = profileUtils.currentUserProfile(request);

    if (maybeProfile.isEmpty()) {
      return CompletableFuture.completedFuture(ok(profileView.renderNoProfile()));
    }

    return maybeProfile
        .get()
        .getApplicant()
        .thenApplyAsync(
            applicant -> ok(profileView.render(maybeProfile.get(), applicant)),
            httpExecutionContext.current());
  }

  public Result profilePage(Http.Request request, Long id) {
    throw new UnsupportedOperationException("Not implemented yet.");
  }
}
