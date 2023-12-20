package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import controllers.CiviFormController;
import controllers.applicant.ApplicantRoutes;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.mvc.Call;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;

/** Controller for admins previewing a program as an applicant. */
public final class AdminProgramPreviewController extends CiviFormController {
  private final ApplicantRoutes applicantRoutes;

  @Inject
  public AdminProgramPreviewController(
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      ApplicantRoutes applicantRoutes) {
    super(profileUtils, versionRepository);
    this.applicantRoutes = checkNotNull(applicantRoutes);
  }

  /**
   * Retrieves the admin's user profile and redirects to the application review page where the admin
   * can preview the program.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result preview(Request request, long programId) {
    CiviFormProfile profile = profileUtils.currentUserProfileOrThrow(request);

    try {
      return redirect(applicantRoutes.review(profile, profile.getApplicant().get().id, programId));
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> back(Request request, long programId) {
    return versionRepository
        .isDraftProgramAsync(programId)
        .thenApplyAsync(
            (isDraftProgram) -> {
              Call reviewPage =
                  controllers.admin.routes.AdminProgramBlocksController.readOnlyIndex(programId);
              if (isDraftProgram) {
                reviewPage = controllers.admin.routes.AdminProgramBlocksController.index(programId);
              }
              return redirect(reviewPage);
            });
  }
}
