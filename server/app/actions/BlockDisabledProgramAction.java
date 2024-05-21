package actions;

import static com.google.common.base.Preconditions.checkNotNull;

import controllers.routes;
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

public class BlockDisabledProgramAction extends Action.Simple {
  private final ProgramService programService;

  @Inject
  public BlockDisabledProgramAction(ProgramService programService) {
    this.programService = checkNotNull(programService);
  }

  @Override
  public CompletionStage<Result> call(Request req) {
    Optional<String> programSlugOptional = req.flash().get("redirected-from-program-slug");

    String programSlug = programSlugOptional.orElse("None");

    if (programSlug.equals("None")) {
      return delegate.call(req);
    }

    ProgramDefinition activeProgramDefinition =
        programService
            .getActiveFullProgramDefinitionAsync(programSlug)
            .toCompletableFuture()
            .join();

    if (activeProgramDefinition.displayMode() == DisplayMode.DISABLED) {
      // TODO: Build an error page and redirect the user to the error page instead
      return CompletableFuture.completedFuture(redirect(routes.HomeController.index()));
    }
    return delegate.call(req);
  }
}
