package actions;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.DisplayMode;
import play.mvc.Action;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.ProgramDefinition;
import services.program.ProgramService;

/**
 * Action that ensures the program the user is trying to access is not disabled.
 *
 * <p>The action will redirect the request to the home page if the the program is disabled.
 *
 * <p>
 */
public class ProgramDisabledAction extends Action.Simple {
  private final ProgramService programService;

  @Inject
  public ProgramDisabledAction(ProgramService programService) {
    this.programService = checkNotNull(programService);
  }

  private boolean programIsDisabled(String programSlug) {
    ProgramDefinition programDefiniton =
        programService
            .getActiveFullProgramDefinitionAsync(programSlug)
            .toCompletableFuture()
            .join();
    return programDefiniton.displayMode() == DisplayMode.DISABLED;
  }

  @Override
  public CompletionStage<Result> call(Request req) {
    Optional<String> programSlugOptional = req.flash().get("redirected-from-program-slug");

    if (programSlugOptional.isPresent() && programIsDisabled(programSlugOptional.get())) {
      return CompletableFuture.completedFuture(
          redirect(
              controllers.applicant.routes.ApplicantProgramsController.showInfoDisabledProgram()));
    }

    return delegate.call(req); // continute processing next step
  }
}
