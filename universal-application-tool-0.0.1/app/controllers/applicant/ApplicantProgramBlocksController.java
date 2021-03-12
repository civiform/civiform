package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.DoNotCall;
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
import views.applicant.ApplicantProgramBlockEditView;

public final class ApplicantProgramBlocksController extends Controller {

  private final ApplicantService applicantService;
  private final MessagesApi messagesApi;
  private final HttpExecutionContext httpExecutionContext;
  private final ApplicantProgramBlockEditView editView;

  @Inject
  public ApplicantProgramBlocksController(
      ApplicantService applicantService,
      MessagesApi messagesApi,
      HttpExecutionContext httpExecutionContext,
      ApplicantProgramBlockEditView editView) {
    this.applicantService = checkNotNull(applicantService);
    this.messagesApi = checkNotNull(messagesApi);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.editView = checkNotNull(editView);
  }

  public CompletionStage<Result> edit(
      Request request, long applicantId, long programId, long blockId) {
    return applicantService
        .getReadOnlyApplicantProgramService(applicantId, programId)
        .thenApplyAsync(
            (roApplicantProgramService) -> {
              Optional<Block> block = roApplicantProgramService.getBlock(blockId);

              if (block.isPresent()) {
                return ok(
                    editView.render(
                        ApplicantProgramBlockEditView.Params.builder()
                            .setRequest(request)
                            .setMessages(messagesApi.preferred(request))
                            .setApplicantId(applicantId)
                            .setProgramId(programId)
                            .setBlock(block.get())
                            .build()));
              } else {
                return notFound();
              }
            },
            httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof ProgramNotFoundException) {
                  return notFound(cause.toString());
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }

  @DoNotCall
  public CompletionStage<Result> update(
      Request request, long applicantId, long programId, long blockId) {
    throw new UnsupportedOperationException("Updates aren't implemented yet");
  }
}
