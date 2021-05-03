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
import models.Application;
import org.pac4j.play.java.Secure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Call;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.ApplicationRepository;
import services.applicant.ApplicantService;
import services.applicant.Block;
import services.applicant.ProgramBlockNotFoundException;
import services.applicant.ReadOnlyApplicantProgramService;
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
  private final ApplicationRepository applicationRepository;
  private final ProfileUtils profileUtils;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject
  public ApplicantProgramBlocksController(
      ApplicantService applicantService,
      MessagesApi messagesApi,
      HttpExecutionContext httpExecutionContext,
      ApplicantProgramBlockEditView editView,
      FormFactory formFactory,
      ApplicationRepository applicationRepository,
      ProfileUtils profileUtils) {
    this.applicantService = checkNotNull(applicantService);
    this.messagesApi = checkNotNull(messagesApi);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.editView = checkNotNull(editView);
    this.formFactory = checkNotNull(formFactory);
    this.applicationRepository = checkNotNull(applicationRepository);
    this.profileUtils = checkNotNull(profileUtils);
  }

  @Secure
  public CompletionStage<Result> edit(
      Request request, long applicantId, long programId, String blockId) {
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
      Request request, long applicantId, long programId, String blockId) {
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

              return update(request, applicantId, programId, blockId, roApplicantProgramService);
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
      ReadOnlyApplicantProgramService roApplicantProgramService) {
    Optional<Block> thisBlockUpdatedMaybe = roApplicantProgramService.getBlock(blockId);
    if (thisBlockUpdatedMaybe.isEmpty()) {
      return failedFuture(new ProgramBlockNotFoundException(programId, blockId));
    }
    Block thisBlockUpdated = thisBlockUpdatedMaybe.get();
    Messages applicantMessages = messagesApi.preferred(request);

    // Validation errors: re-render this block with errors and previously entered data.
    if (thisBlockUpdated.hasErrors()) {
      return supplyAsync(
          () ->
              ok(
                  editView.render(
                      ApplicantProgramBlockEditView.Params.builder()
                          .setRequest(request)
                          .setMessages(applicantMessages)
                          .setApplicantId(applicantId)
                          .setProgramId(programId)
                          .setBlock(thisBlockUpdated)
                          .setPreferredLanguageSupported(
                              roApplicantProgramService.preferredLanguageSupported())
                          .build())));
    }

    // TODO(https://github.com/seattle-uat/universal-application-tool/issues/256): Redirect to
    //  review page when it is available.
    Optional<String> nextBlockIdMaybe =
        roApplicantProgramService.getBlockAfter(blockId).map(Block::getId);
    return nextBlockIdMaybe.isEmpty()
        ? previewPageRedirect(applicantMessages, applicantId, programId)
        : supplyAsync(
            () ->
                redirect(
                    routes.ApplicantProgramBlocksController.edit(
                        applicantId, programId, nextBlockIdMaybe.get())));
  }

  private CompletionStage<Result> previewPageRedirect(
      Messages messages, long applicantId, long programId) {
    // TODO(https://github.com/seattle-uat/universal-application-tool/issues/256): Replace
    // with a redirect to the review page.
    // For now, this just saves the application and redirects to program index page.
    Call endOfProgramSubmission = routes.ApplicantProgramsController.index(applicantId);
    logger.debug("redirecting to preview page with %d, %d", applicantId, programId);
    return submit(applicantId, programId)
        .thenApplyAsync(
            applicationMaybe -> {
              if (applicationMaybe.isEmpty()) {
                return found(endOfProgramSubmission)
                    .flashing("banner", "Error saving application.");
              }
              Application application = applicationMaybe.get();
              // Placeholder application ID display.
              return found(endOfProgramSubmission)
                  .flashing("banner", messages.at("toast.applicationSaved", application.id));
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
