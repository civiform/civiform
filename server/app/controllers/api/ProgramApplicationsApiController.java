package controllers.api;

import static com.google.common.base.Preconditions.checkNotNull;

import controllers.CiviFormController;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.mvc.Http;
import play.mvc.Result;
import services.program.ProgramService;

public final class ProgramApplicationsApiController extends CiviFormController {

  private final ProgramService programService;

  @Inject
  public ProgramApplicationsApiController(ProgramService programService) {
    this.programService = checkNotNull(programService);
  }

  public CompletionStage<Result> list(Http.Request request, Long programId) {
    programService.getAllProgramNames();
    return CompletableFuture.completedFuture(ok());
  }
}
