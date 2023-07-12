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
import views.api.ApiDocsView;

public class ApiDocsController {

  private final ApiDocsView docsView;
  private final ProgramService programService;

  @Inject
  public ApiDocsController(ApiDocsView docsView, ProgramService programService) {
    this.docsView = docsView;
    this.programService = programService;
  }

  public Result index(Http.Request request) {
    Optional<String> firstProgramSlug = programService.getAllProgramSlugs().stream().findFirst();
    return firstProgramSlug
        .map(slug -> redirect(routes.ApiDocsController.docsForSlug(slug)))
        .orElse(notFound("No programs found"));
  }

  public Result docsForSlug(Http.Request request, String programSlug) {
    ImmutableSet<String> allProgramSlugs = programService.getAllProgramSlugs();
    ProgramDefinition programDefinition =
        programService.getActiveProgramDefinitionAsync(programSlug).toCompletableFuture().join();
    return ok(docsView.render(request, programDefinition, allProgramSlugs));
  }
}
