package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Call;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.ApplicationRepository;
import services.applicant.ApplicantService;
import services.applicant.Block;
import services.applicant.ReadOnlyApplicantProgramService;
import services.program.ProgramBlockNotFoundException;
import services.program.ProgramNotFoundException;
import views.applicant.ApplicantProgramBlockEditView;

public final class ApplicantProgramBlocksController extends Controller {
  private static final ImmutableSet<String> STRIPPED_FORM_FIELDS = ImmutableSet.of("csrfToken");

  private final ApplicantService applicantService;
  private final MessagesApi messagesApi;
  private final HttpExecutionContext httpExecutionContext;
  private final ApplicantProgramBlockEditView editView;
  private final FormFactory formFactory;
  private final ApplicationRepository applicationRepository;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject
  public ApplicantProgramBlocksController(
      ApplicantService applicantService,
      MessagesApi messagesApi,
      HttpExecutionContext httpExecutionContext,
      ApplicantProgramBlockEditView editView,
      FormFactory formFactory,
      ApplicationRepository applicationRepository) {
    this.applicantService = checkNotNull(applicantService);
    this.messagesApi = checkNotNull(messagesApi);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.editView = checkNotNull(editView);
    this.formFactory = checkNotNull(formFactory);
    this.applicationRepository = checkNotNull(applicationRepository);
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

  public CompletionStage<Result> update(
      Request request, long applicantId, long programId, long blockId) {
    DynamicForm form = formFactory.form().bindFromRequest(request);
    ImmutableMap<String, String> formData = cleanForm(form.rawData());

    return applicantService
        .stageAndUpdateIfValid(applicantId, programId, blockId, formData)
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

              return update(request, applicantId, programId, blockId, roApplicantProgramService);
            },
            httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof ProgramNotFoundException) {
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
      long blockId,
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
                          .build())));
    }

    // TODO(https://github.com/seattle-uat/universal-application-tool/issues/256): Redirect to
    //  review page when it is available.
    Optional<Long> nextBlockIdMaybe =
        roApplicantProgramService.getBlockAfter(blockId).map(Block::getId);
    return nextBlockIdMaybe.isEmpty()
        ? previewPageRedirect(applicantId, programId)
        : supplyAsync(
            () ->
                redirect(
                    routes.ApplicantProgramBlocksController.edit(
                        applicantId, programId, nextBlockIdMaybe.get())));
  }

  private CompletionStage<Result> previewPageRedirect(long applicantId, long programId) {
    // TODO(https://github.com/seattle-uat/universal-application-tool/issues/256): Replace
    // with a redirect to the review page.
    // For now, this just saves the application and redirects to program index page.
    Call endOfProgramSubmission = routes.ApplicantProgramsController.index(applicantId);
    logger.debug("redirecting to preview page with %d, %d", applicantId, programId);
    return submit(applicantId, programId)
        .thenApplyAsync(
            applicationMaybe -> {
              if (applicationMaybe.isEmpty()) {
                return found(endOfProgramSubmission).flashing("banner", "Error saving program.");
              }
              Application application = applicationMaybe.get();
              // Placeholder application ID display.
              return found(endOfProgramSubmission)
                  .flashing(
                      "banner",
                      String.format(
                          "Successfully saved application: application ID %d", application.id));
            });
  }

  private CompletionStage<Optional<Application>> submit(long applicantId, long programId) {
    return applicationRepository.submitApplication(applicantId, programId);
  }

  private ImmutableMap<String, String> cleanForm(Map<String, String> formData) {
    return formData.entrySet().stream()
        .filter(entry -> !STRIPPED_FORM_FIELDS.contains(entry.getKey()))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
