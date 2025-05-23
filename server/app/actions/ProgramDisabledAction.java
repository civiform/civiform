package actions;

import static com.google.common.base.Preconditions.checkNotNull;

import controllers.FlashKey;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.DisplayMode;
import play.mvc.Action;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.routing.Router;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
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
    try {
      ProgramDefinition programDefiniton =
          programService
              .getActiveFullProgramDefinitionAsync(programSlug)
              .toCompletableFuture()
              .join();

      return programDefiniton.displayMode() == DisplayMode.DISABLED;
    } catch (RuntimeException e) {
      // No active program was found. Return false here on error and
      // let the rest of the application take over to handle programs
      // that are missing or only have a draft version
      return false;
    }
  }

  private ProgramDefinition getProgram(long programId) {
    try {
      return programService.getFullProgramDefinition(programId);
    } catch (ProgramNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public CompletionStage<Result> call(Request req) {

    Optional<String> programSlugOptional = getProgramSlug(req);

    if (programSlugOptional.isPresent() && programIsDisabled(programSlugOptional.get())) {
      return CompletableFuture.completedFuture(
          redirect(
              controllers.applicant.routes.ApplicantProgramsController.showInfoDisabledProgram(
                  programSlugOptional.get())));
    }

    return delegate.call(req); // continute processing next step
  }

  /** Get the program slug from flash key or from programId */
  private Optional<String> getProgramSlug(Request request) {
    Optional<String> programSlugOptional =
        request.flash().get(FlashKey.REDIRECTED_FROM_PROGRAM_SLUG);

    if (programSlugOptional.isPresent()) {
      return programSlugOptional;
    }

    if (!request.attrs().containsKey(Router.Attrs.HANDLER_DEF)) {
      return Optional.empty();
    }

    String routePattern = request.attrs().get(Router.Attrs.HANDLER_DEF).path();
    RouteExtractor routeExtractor = new RouteExtractor(routePattern, request.path());
    Optional<Long> programId = routeExtractor.getParamOptionalLongValue("programId");

    if (programId.isPresent()) {
      ProgramDefinition program = getProgram(programId.get());
      return Optional.of(program.slug());
    }

    return Optional.empty();
  }
}
