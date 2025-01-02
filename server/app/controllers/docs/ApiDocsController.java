package controllers.docs;

import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;
import static play.mvc.Results.redirect;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import javax.inject.Inject;
import play.mvc.Http;
import play.mvc.Result;
import services.program.ProgramDefinition;
import services.program.ProgramDraftNotFoundException;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import views.docs.ApiDocsView;

public final class ApiDocsController {

  private final ApiDocsView docsView;
  private final ProgramService programService;
  private final SettingsManifest settingsManifest;

  @Inject
  public ApiDocsController(
      ApiDocsView docsView, ProgramService programService, SettingsManifest settingsManifest) {
    this.docsView = docsView;
    this.programService = programService;
    this.settingsManifest = settingsManifest;
  }

  /**
   * Like {@link #docsForSlug}, but defaults to an arbitrary program when one is not set in the URL.
   */
  public Result index(Http.Request request) {
    Optional<String> firstProgramSlug = programService.getAllProgramSlugs().stream().findFirst();
    return firstProgramSlug
        .map(slug -> redirect(routes.ApiDocsController.activeDocsForSlug(slug)))
        .orElse(
            notFound(
                "No programs found. Please create and publish a program before accessing"
                    + " API docs."));
  }

  public Result activeDocsForSlug(Http.Request request, String selectedProgramSlug) {
    return docsForSlug(request, selectedProgramSlug, /* useActiveVersion= */ true);
  }

  public Result draftDocsForSlug(Http.Request request, String selectedProgramSlug) {
    return docsForSlug(request, selectedProgramSlug, /* useActiveVersion= */ false);
  }

  private Result docsForSlug(
      Http.Request request, String selectedProgramSlug, boolean useActiveVersion) {
    if (!settingsManifest.getApiGeneratedDocsEnabled(request)) {
      return notFound("API Docs are not enabled.");
    }

    ImmutableSet<String> allProgramSlugs = programService.getAllProgramSlugs();
    Optional<ProgramDefinition> programDefinition =
        getProgramDefinition(selectedProgramSlug, useActiveVersion);

    return ok(docsView.render(request, selectedProgramSlug, programDefinition, allProgramSlugs));
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
