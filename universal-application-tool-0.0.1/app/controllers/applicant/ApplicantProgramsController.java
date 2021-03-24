package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Call;
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
  private final ProgramIndexView programIndexView;

  @Inject
  public ApplicantProgramsController(
      HttpExecutionContext httpContext,
      ApplicantService applicantService,
      MessagesApi messagesApi,
      ProgramIndexView programIndexView) {
    this.httpContext = httpContext;
    this.applicantService = applicantService;
    this.messagesApi = checkNotNull(messagesApi);
    this.programIndexView = checkNotNull(programIndexView);
  }

  public CompletionStage<Result> index(Request request, long applicantId) {
    Optional<String> banner = request.flash().get("banner");
    return applicantService
        .relevantPrograms(applicantId)
        .thenApplyAsync(
            programs -> {
              return ok(
                  programIndexView.render(
                      messagesApi.preferred(request), applicantId, programs, banner));
            },
            httpContext.current());
  }

  public CompletionStage<Result> edit(long applicantId, long programId) {
    // Determine first incomplete block, then redirect to other edit.
    return applicantService
        .getReadOnlyApplicantProgramService(applicantId, programId)
        .thenApplyAsync(
            roApplicantService -> {
              Optional<Block> blockMaybe = roApplicantService.getFirstIncompleteBlock();
              return blockMaybe.flatMap(
                  block ->
                      Optional.of(
                          found(
                              routes.ApplicantProgramBlocksController.edit(
                                  applicantId, programId, block.getId()))));
            },
            httpContext.current())
        .thenComposeAsync(
            resultMaybe -> {
              if (resultMaybe.isEmpty()) {
                return previewPageRedirect(applicantId);
              }
              return supplyAsync(resultMaybe::get);
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

  private CompletionStage<Result> previewPageRedirect(long applicantId) {
    // TODO(https://github.com/seattle-uat/universal-application-tool/issues/256): Replace
    // with a redirect to the review page.
    // For now, this just redirects to program index page.
    Call endOfProgramSubmission = routes.ApplicantProgramsController.index(applicantId);
    return supplyAsync(
        () ->
            found(endOfProgramSubmission)
                .flashing("banner", String.format("Application was already completed.")));
  }
}
