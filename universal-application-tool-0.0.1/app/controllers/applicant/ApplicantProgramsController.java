package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;
import services.program.ProgramService;
import views.applicant.ProgramIndexView;

/** Controller for handling methods for an applicant applying to programs. */
public class ApplicantProgramsController extends Controller {

  private final HttpExecutionContext httpContext;
  private final ProgramService programService;
  private final ProgramIndexView programIndexView;

  @Inject
  public ApplicantProgramsController(
      HttpExecutionContext httpContext,
      ProgramService programService,
      ProgramIndexView programIndexView) {
    this.httpContext = httpContext;
    this.programService = checkNotNull(programService);
    this.programIndexView = checkNotNull(programIndexView);
  }

  public CompletionStage<Result> index(long applicantId) {
    return programService
        .listProgramDefinitionsAsync()
        .thenApplyAsync(
            programs -> ok(programIndexView.render(applicantId, programs)), httpContext.current());
  }

  // TODO(https://github.com/seattle-uat/universal-application-tool/issues/224): Go to the next
  // block for the chosen program
  public CompletionStage<Result> edit(long applicantId, long programId) {
    return CompletableFuture.completedFuture(
        ok("Applicant " + applicantId + " chose program " + programId));
  }
}
