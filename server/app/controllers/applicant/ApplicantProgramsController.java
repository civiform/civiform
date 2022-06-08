package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableSet;
import controllers.CiviFormController;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.LifecycleStage;
import org.pac4j.play.java.Secure;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.applicant.ApplicantService;
import services.applicant.Block;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.applicant.ApplicantProgramInfoView;
import views.applicant.ProgramIndexView;

/**
 * Controller for handling methods for an applicant applying to programs. CAUTION: you must
 * explicitly check the current profile so that an unauthorized user cannot access another
 * applicant's data!
 */
public class ApplicantProgramsController extends CiviFormController {

  private final HttpExecutionContext httpContext;
  private final ApplicantService applicantService;
  private final ProgramService programService;
  private final MessagesApi messagesApi;
  private final ProgramIndexView programIndexView;
  private final ApplicantProgramInfoView programInfoView;
  private final ProfileUtils profileUtils;

  @Inject
  public ApplicantProgramsController(
      HttpExecutionContext httpContext,
      ApplicantService applicantService,
      ProgramService programService,
      MessagesApi messagesApi,
      ProgramIndexView programIndexView,
      ApplicantProgramInfoView programInfoView,
      ProfileUtils profileUtils) {
    this.httpContext = checkNotNull(httpContext);
    this.applicantService = checkNotNull(applicantService);
    this.programService = checkNotNull(programService);
    this.messagesApi = checkNotNull(messagesApi);
    this.programIndexView = checkNotNull(programIndexView);
    this.programInfoView = checkNotNull(programInfoView);
    this.profileUtils = checkNotNull(profileUtils);
  }

  @Secure
  public CompletionStage<Result> index(Request request, long applicantId) {
    Optional<String> banner = request.flash().get("banner");
    CompletionStage<Optional<String>> applicantStage = this.applicantService.getName(applicantId);

    return applicantStage
        .thenComposeAsync(v -> checkApplicantAuthorization(profileUtils, request, applicantId))
        .thenComposeAsync(
            v -> {
              return applicantService.relevantProgramsForApplicant(
                applicantId, ImmutableSet.of(LifecycleStage.ACTIVE, LifecycleStage.DRAFT));
            },
            httpContext.current())
        .thenApplyAsync(
            relevantPrograms -> {
              return ok(programIndexView.render(
                      messagesApi.preferred(request),
                      request,
                      applicantId,
                      applicantStage.toCompletableFuture().join(),
                      relevantPrograms,
                      banner));
            },
            httpContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                if (ex.getCause() instanceof SecurityException) {
                  return unauthorized();
                }
              }
              throw new RuntimeException(ex);
            });
  }

  @Secure
  public CompletionStage<Result> view(Request request, long applicantId, long programId) {
    CompletionStage<Optional<String>> applicantStage = this.applicantService.getName(applicantId);

    return applicantStage
        .thenComposeAsync(v -> checkApplicantAuthorization(profileUtils, request, applicantId))
        .thenComposeAsync(
            v -> programService.getProgramDefinitionAsync(programId), httpContext.current())
        .thenApplyAsync(
            programDefinition -> {
              return ok(
                  programInfoView.render(
                      messagesApi.preferred(request),
                      programDefinition,
                      request,
                      applicantId,
                      applicantStage.toCompletableFuture().join()));
            },
            httpContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                if (ex.getCause() instanceof SecurityException) {
                  return unauthorized();
                } else if (ex.getCause() instanceof ProgramNotFoundException) {
                  return badRequest();
                }
              }
              throw new RuntimeException(ex);
            });
  }

  @Secure
  public CompletionStage<Result> edit(Request request, long applicantId, long programId) {

    // Determine first incomplete block, then redirect to other edit.
    return checkApplicantAuthorization(profileUtils, request, applicantId)
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId))
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
                return supplyAsync(
                    () ->
                        redirect(
                            routes.ApplicantProgramReviewController.review(
                                applicantId, programId)));
              }
              return supplyAsync(resultMaybe::get);
            },
            httpContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof SecurityException) {
                  return unauthorized();
                }
                if (cause instanceof ProgramNotFoundException) {
                  return badRequest(cause.toString());
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }
}
