package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;
import services.applicant.ApplicantService;
import services.applicant.Block;
import services.program.ProgramService;
import views.applicant.ProgramIndexView;

/** Controller for handling methods for an applicant applying to programs. */
public class ApplicantProgramsController extends Controller {

  private final HttpExecutionContext httpContext;
  private final ApplicantService applicantService;
  private final ProgramService programService;
  private final ProgramIndexView programIndexView;

  @Inject
  public ApplicantProgramsController(
      HttpExecutionContext httpContext,
      ApplicantService applicantService,
      ProgramService programService,
      ProgramIndexView programIndexView) {
    this.httpContext = httpContext;
    this.applicantService = applicantService;
    this.programService = checkNotNull(programService);
    this.programIndexView = checkNotNull(programIndexView);
  }

  public CompletionStage<Result> index(long applicantId) {
    return programService
        .listProgramDefinitionsAsync()
        .thenApplyAsync(
            programs -> ok(programIndexView.render(applicantId, programs)), httpContext.current());
  }

  // TODO(https://github.com/seattle-uat/universal-application-tool/issues/224): Get next incomplete
  // block instead of just first block.
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
                        applicantId, programId, block.get().id()));
              } else {
                // TODO(https://github.com/seattle-uat/universal-application-tool/issues/256): All
                // blocks are filled in, so redirect to end of program submission.
                return found(routes.ApplicantProgramsController.index(applicantId));
              }
            },
            httpContext.current());
  }
}
