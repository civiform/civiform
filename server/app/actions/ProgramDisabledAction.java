package actions;

import static com.google.common.base.Preconditions.checkNotNull;

import controllers.FlashKey;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.DisplayMode;
import org.apache.commons.lang3.StringUtils;
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

  @Override
  public CompletionStage<Result> call(Request req) {

    Optional<String> programSlugOptional = getProgramSlug(req);

    if (programSlugOptional.isPresent() && programIsDisabled(programSlugOptional.get())) {
      return CompletableFuture.completedFuture(
          redirect(
              controllers.applicant.routes.ApplicantProgramsController.showInfoDisabledProgram(
                  programSlugOptional.get())));
    }

    // Program is not disabled or not found - continue with normal processing
    return delegate.call(req);
  }

  /** Extracts the program slug from the HTTP request flash key or route parameter. */
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

    if (routeExtractor.containsKey("programParam")) {
      return extractSlugFromProgramParam(routeExtractor);
    }

    if (routeExtractor.containsKey("programId")) {
      return extractSlugFromProgramId(routeExtractor);
    }

    return Optional.empty();
  }

  /**
   * Extracts program slug from the programParam route parameter. Handles both string slugs and
   * numeric IDs.
   */
  private Optional<String> extractSlugFromProgramParam(RouteExtractor routeExtractor) {
    try {
      String programParam = routeExtractor.getParamStringValue("programParam");

      if (StringUtils.isNumeric(programParam)) {
        long programId = Long.parseLong(programParam);
        ProgramDefinition program = programService.getFullProgramDefinition(programId);
        return Optional.of(program.slug());
      }

      return Optional.of(programParam);

    } catch (ProgramNotFoundException | RuntimeException e) {
      return Optional.empty();
    }
  }

  /** Extracts program slug from the programId route parameter. */
  private Optional<String> extractSlugFromProgramId(RouteExtractor routeExtractor) {
    try {
      Long programId = routeExtractor.getParamLongValue("programId");
      ProgramDefinition program = programService.getFullProgramDefinition(programId);
      return Optional.of(program.slug());
    } catch (ProgramNotFoundException | RuntimeException e) {
      return Optional.empty();
    }
  }

  /** Returns whether the program is disabled. */
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
}
