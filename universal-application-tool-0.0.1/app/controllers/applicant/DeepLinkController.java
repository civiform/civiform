package controllers.applicant;

import auth.ProfileUtils;
import auth.UatProfile;
import com.google.common.base.Preconditions;
import controllers.CiviFormController;
import controllers.routes;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import models.Applicant;
import models.Program;
import org.pac4j.play.java.Secure;
import play.mvc.Http;
import play.mvc.Result;
import repository.ProgramRepository;

public class DeepLinkController extends CiviFormController {
  private ProfileUtils profileUtils;
  private ProgramRepository programRepository;

  @Inject
  public DeepLinkController(ProfileUtils profileUtils, ProgramRepository programRepository) {
    this.profileUtils = Preconditions.checkNotNull(profileUtils);
    this.programRepository = Preconditions.checkNotNull(programRepository);
  }

  @Secure
  public CompletableFuture<Result> programByName(Http.Request request, String programName) {
    Optional<UatProfile> profile = profileUtils.currentUserProfile(request);
    if (profile.isEmpty()) {
      return CompletableFuture.completedFuture(
          redirect(routes.CallbackController.callback("GuestClient")));
    }
    CompletableFuture<Applicant> applicant = profile.get().getApplicant();
    CompletableFuture<Program> program = programRepository.getForSlug(programName);
    return CompletableFuture.allOf(applicant, program)
        .thenApply(
            empty -> {
              if (applicant.isCompletedExceptionally()) {
                return notFound();
              } else if (program.isCompletedExceptionally()) {
                return notFound();
              }
              return redirect(
                  controllers.applicant.routes.ApplicantProgramsController.edit(
                      applicant.join().id, program.join().id));
            });
  }
}
