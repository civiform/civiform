package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.applicant.ApplicantService;
import services.applicant.Block;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.applicant.ProgramIndexView;

/** Controller for handling methods for an applicant applying to programs. */
public class ApplicantProgramsController extends Controller {

  private final HttpExecutionContext httpContext;
  private final ApplicantService applicantService;
  private final MessagesApi messagesApi;
  private final ProgramService programService;
  private final ProgramIndexView programIndexView;

  @Inject
  public ApplicantProgramsController(
      HttpExecutionContext httpContext,
      ApplicantService applicantService,
      MessagesApi messagesApi,
      ProgramService programService,
      ProgramIndexView programIndexView) {
    this.httpContext = httpContext;
    this.applicantService = applicantService;
    this.messagesApi = checkNotNull(messagesApi);
    this.programService = checkNotNull(programService);
    this.programIndexView = checkNotNull(programIndexView);
  }

  public CompletionStage<Result> index(Request request, long applicantId) {
    return programService
        .listProgramDefinitionsAsync()
        .thenApplyAsync(
            programs -> {
              return ok(
                  programIndexView.render(messagesApi.preferred(request), applicantId, programs));
            },
            httpContext.current());
  }

  public CompletionStage<Result> edit(long applicantId, long programId) {
    // Determine first incomplete block, then redirect to other edit.
    return applicantService
        .getReadOnlyApplicantProgramService(applicantId, programId)
        .thenApplyAsync(
            roApplicantService -> {
              Optional<Block> block = roApplicantService.getFirstIncompleteBlock();
              if (block.isPresent()) {
                return found(
                    routes.ApplicantProgramBlocksController.edit(
                        applicantId, programId, block.get().getId()));
              } else {
                // TODO(https://github.com/seattle-uat/universal-application-tool/issues/256): All
                // blocks are filled in, so redirect to end of program submission.
                return found(routes.ApplicantProgramsController.index(applicantId));
              }
            },
            httpContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof ProgramNotFoundException) {
                  return badRequest(cause.toString());
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }
}
