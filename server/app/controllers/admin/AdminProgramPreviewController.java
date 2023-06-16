package controllers.admin;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import controllers.CiviFormController;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.mvc.Http.Request;
import play.mvc.Result;

public final class AdminProgramPreviewController extends CiviFormController {

  private final ProfileUtils profileUtils;

  @Inject
  public AdminProgramPreviewController(ProfileUtils profileUtils) {
    this.profileUtils = profileUtils;
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result preview(Request request, long programId) throws Exception {
    Optional<CiviFormProfile> profile = profileUtils.currentUserProfile(request);

    if (profile.isEmpty()) {

      throw new Exception();
      /*
      Result result = redirect(routes.CallbackController.callback(GuestClient.CLIENT_NAME).url());
      result = result.withSession(ImmutableMap.of("redirectTo", request.uri()));
      return CompletableFuture.completedFuture(result);
      */
    }
    return redirect(
        controllers.applicant.routes.ApplicantProgramReviewController.review(
            profile.get().getApplicant().get().id, programId));
  }
}