package controllers.api;

import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;
import static play.mvc.Results.redirect;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import javax.inject.Inject;
import play.mvc.Http;
import play.mvc.Result;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import views.api.ApiDocsView;

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
        .map(slug -> redirect(routes.ApiDocsController.docsForSlug(slug)))
        .orElse(
            notFound(
                "No active programs found. Please create and publish a program before accessing"
                    + " API docs."));
  }

  public Result docsForSlug(Http.Request request, String programSlug) {
    if (!settingsManifest.getApiGeneratedDocsEnabled()) {
      return notFound("API Docs are not enabled.");
    }

    ImmutableSet<String> allProgramSlugs = programService.getAllProgramSlugs();

    ProgramDefinition programDefinition;
    try {
      programDefinition =
          programService.getActiveProgramDefinitionAsync(programSlug).toCompletableFuture().join();
    } catch (Exception e) {
      return notFound(
          String.format(
              "No active programs found for %s. Please create and publish a program with this slug"
                  + " to continue.",
              programSlug));
    }

    return ok(docsView.render(request, programDefinition, allProgramSlugs));
  }
}
