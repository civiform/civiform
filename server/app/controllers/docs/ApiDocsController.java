package controllers.docs;

import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;
import static play.mvc.Results.redirect;

import auth.Authorizers;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.mvc.Http;
import play.mvc.Result;
import services.program.ProgramDefinition;
import services.program.ProgramDraftNotFoundException;
import services.program.ProgramService;
import views.docs.ApiDocsView;

public final class ApiDocsController {

  private final ApiDocsView docsView;
  private final ProgramService programService;

  @Inject
  public ApiDocsController(ApiDocsView docsView, ProgramService programService) {
    this.docsView = docsView;
    this.programService = programService;
  }

  /**
   * Like {@link #docsForSlug}, but defaults to an arbitrary program when one is not set in the URL.
   */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result index(Http.Request request) {
    Optional<String> firstProgramSlug =
        programService.getAllNonExternalProgramSlugs().stream().findFirst();
    return firstProgramSlug
        .map(slug -> redirect(routes.ApiDocsController.activeDocsForSlug(slug)))
        .orElse(
            notFound(
                "No programs found. Please create and publish a program before accessing"
                    + " API docs."));
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result activeDocsForSlug(Http.Request request, String selectedProgramSlug) {
    return docsForSlug(request, selectedProgramSlug, /* useActiveVersion= */ true);
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result draftDocsForSlug(Http.Request request, String selectedProgramSlug) {
    return docsForSlug(request, selectedProgramSlug, /* useActiveVersion= */ false);
  }

  private Result docsForSlug(
      Http.Request request, String selectedProgramSlug, boolean useActiveVersion) {
    ImmutableSet<String> allNonExternalProgramSlugs =
        programService.getAllNonExternalProgramSlugs();
    Optional<ProgramDefinition> programDefinition =
        allNonExternalProgramSlugs.contains(selectedProgramSlug)
            ? getProgramDefinition(selectedProgramSlug, useActiveVersion)
            : Optional.empty();

    return ok(
        docsView.render(
            request, selectedProgramSlug, programDefinition, allNonExternalProgramSlugs));
  }

  private Optional<ProgramDefinition> getProgramDefinition(
      String programSlug, boolean useActiveVersion) {

    try {
      if (useActiveVersion) {
        ProgramDefinition activeProgramDefinition =
            programService
                .getActiveFullProgramDefinitionAsync(programSlug)
                .toCompletableFuture()
                .join();
        return Optional.of(activeProgramDefinition);
      } else {
        ProgramDefinition draftProgramDefinition =
            programService.getDraftFullProgramDefinition(programSlug);
        return Optional.of(draftProgramDefinition);
      }

    } catch (RuntimeException | ProgramDraftNotFoundException e) {
      return Optional.empty();
    }
  }
}
