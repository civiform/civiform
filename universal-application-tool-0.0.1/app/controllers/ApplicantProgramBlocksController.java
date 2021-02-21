package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.mvc.Http.Request;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;
import services.applicant.ApplicantService;
import services.applicant.Block;
import views.ApplicantProgramBlockEditView;

public final class ApplicantProgramBlocksController extends Controller {

  private final ApplicantService applicantService;
  private final HttpExecutionContext httpExecutionContext;
  private final ApplicantProgramBlockEditView editView;

  @Inject
  public ApplicantProgramBlocksController(ApplicantService applicantService,
      HttpExecutionContext httpExecutionContext, ApplicantProgramBlockEditView editView) {
    this.applicantService = checkNotNull(applicantService);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.editView = checkNotNull(editView);
  }

  public CompletionStage<Result> edit(Request request, long applicantId, long programId,
      long blockId) {
    return applicantService.getReadOnlyApplicantProgramService(applicantId, programId)
        .thenApplyAsync((roApplicantProgramService) -> {
          Optional<Block> block = roApplicantProgramService.getBlock(blockId);

          if (block.isPresent()) {
            return ok(editView.render(request, block.get()));
          } else {
            throw new RuntimeException("That block isn't available");
          }
        }, httpExecutionContext.current());
  }
}
