package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import controllers.CiviFormController;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.applicant.ApplicantService;
import services.applicant.Block;
import services.applicant.ReadOnlyApplicantProgramService;
import services.program.ProgramBlockNotFoundException;
import services.program.ProgramNotFoundException;
import views.applicant.ApplicantProgramBlockEditView;

/**
 * Controller for handling an applicant filling out a single program. CAUTION: you must explicitly
 * check the current profile so that an unauthorized user cannot access another applicant's data!
 */
public final class ApplicantProgramBlocksController extends CiviFormController {
  private static final ImmutableSet<String> STRIPPED_FORM_FIELDS = ImmutableSet.of("csrfToken");

  private final ApplicantService applicantService;
  private final MessagesApi messagesApi;
  private final HttpExecutionContext httpExecutionContext;
  private final ApplicantProgramBlockEditView editView;
  private final FormFactory formFactory;
  private final ProfileUtils profileUtils;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject
  public ApplicantProgramBlocksController(
      ApplicantService applicantService,
      MessagesApi messagesApi,
      HttpExecutionContext httpExecutionContext,
      ApplicantProgramBlockEditView editView,
      FormFactory formFactory,
      ProfileUtils profileUtils) {
    this.applicantService = checkNotNull(applicantService);
    this.messagesApi = checkNotNull(messagesApi);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.editView = checkNotNull(editView);
    this.formFactory = checkNotNull(formFactory);
    this.profileUtils = checkNotNull(profileUtils);
  }

  @Secure
  public CompletionStage<Result> edit(
      Request request, long applicantId, long programId, String blockId) {
    return editOrReview(request, applicantId, programId, blockId, false);
  }

  @Secure
  public CompletionStage<Result> review(
      Request request, long applicantId, long programId, String blockId) {
    return editOrReview(request, applicantId, programId, blockId, true);
  }

  @Secure
  private CompletionStage<Result> editOrReview(
      Request request, long applicantId, long programId, String blockId, boolean inReview) {
    return checkApplicantAuthorization(profileUtils, request, applicantId)
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            httpExecutionContext.current())
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
                            .setInReview(inReview)
                            .setPreferredLanguageSupported(
                                roApplicantProgramService.preferredLanguageSupported())
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
                if (cause instanceof SecurityException) {
                  return unauthorized();
                }
                if (cause instanceof ProgramNotFoundException) {
                  return notFound(cause.toString());
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }

  @Secure
  public CompletionStage<Result> update(
      Request request, long applicantId, long programId, String blockId, boolean inReview) {
    return checkApplicantAuthorization(profileUtils, request, applicantId)
        .thenComposeAsync(
            v -> {
              DynamicForm form = formFactory.form().bindFromRequest(request);
              ImmutableMap<String, String> formData = cleanForm(form.rawData());

              return applicantService.stageAndUpdateIfValid(
                  applicantId, programId, blockId, formData);
            },
            httpExecutionContext.current())
        .thenComposeAsync(
            (errorAndROApplicantProgramService) -> {
              if (errorAndROApplicantProgramService.isError()) {
                errorAndROApplicantProgramService
                    .getErrors()
                    .forEach(e -> logger.error("Exception while updating applicant data", e));
                return supplyAsync(() -> badRequest("Unable to process this request"));
              }
              ReadOnlyApplicantProgramService roApplicantProgramService =
                  errorAndROApplicantProgramService.getResult();

              return update(
                  request, applicantId, programId, blockId, inReview, roApplicantProgramService);
            },
            httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof SecurityException) {
                  return unauthorized();
                } else if (cause instanceof ProgramNotFoundException) {
                  return badRequest(cause.toString());
                } else if (cause instanceof ProgramBlockNotFoundException) {
                  logger.error("Exception while updating applicant data", cause);
                  return badRequest("Unable to process this request");
                } else if (cause instanceof IllegalArgumentException) {
                  logger.error(cause.getMessage());
                  return badRequest();
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }

  private CompletionStage<Result> update(
      Request request,
      long applicantId,
      long programId,
      String blockId,
      boolean inReview,
      ReadOnlyApplicantProgramService roApplicantProgramService) {
    Optional<Block> thisBlockUpdatedMaybe = roApplicantProgramService.getBlock(blockId);
    if (thisBlockUpdatedMaybe.isEmpty()) {
      return failedFuture(new ProgramBlockNotFoundException(programId, blockId));
    }
    Block thisBlockUpdated = thisBlockUpdatedMaybe.get();

    // Validation errors: re-render this block with errors and previously entered data.
    if (thisBlockUpdated.hasErrors()) {
      return supplyAsync(
          () ->
              ok(
                  editView.render(
                      ApplicantProgramBlockEditView.Params.builder()
                          .setRequest(request)
                          .setMessages(messagesApi.preferred(request))
                          .setApplicantId(applicantId)
                          .setProgramId(programId)
                          .setBlock(thisBlockUpdated)
                          .setInReview(inReview)
                          .setPreferredLanguageSupported(
                              roApplicantProgramService.preferredLanguageSupported())
                          .build())));
    }

    Optional<String> nextBlockIdMaybe =
        roApplicantProgramService.getBlockAfter(blockId).map(Block::getId);
    return nextBlockIdMaybe.isEmpty() || inReview
        ? supplyAsync(
            () -> redirect(routes.ApplicantProgramReviewController.review(applicantId, programId)))
        : supplyAsync(
            () ->
                redirect(
                    routes.ApplicantProgramBlocksController.edit(
                        applicantId, programId, nextBlockIdMaybe.get())));
  }

  private ImmutableMap<String, String> cleanForm(Map<String, String> formData) {
    return formData.entrySet().stream()
        .filter(entry -> !STRIPPED_FORM_FIELDS.contains(entry.getKey()))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
