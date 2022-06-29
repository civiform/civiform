package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import controllers.CiviFormController;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.StoredFile;
import org.pac4j.play.java.Secure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.StoredFileRepository;
import services.applicant.ApplicantService;
import services.applicant.Block;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applicant.exception.ApplicantNotFoundException;
import services.applicant.exception.ProgramBlockNotFoundException;
import services.applicant.question.FileUploadQuestion;
import services.cloud.StorageClient;
import services.program.PathNotInBlockException;
import services.program.ProgramNotFoundException;
import services.question.exceptions.UnsupportedScalarTypeException;
import services.question.types.QuestionType;
import views.ApplicationBaseView;
import views.FileUploadViewStrategy;
import views.applicant.ApplicantProgramBlockEditView;
import views.applicant.ApplicantProgramBlockEditViewFactory;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;

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
  private final StorageClient storageClient;
  private final StoredFileRepository storedFileRepository;
  private final ProfileUtils profileUtils;
  private final String baseUrl;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject
  public ApplicantProgramBlocksController(
      ApplicantService applicantService,
      MessagesApi messagesApi,
      HttpExecutionContext httpExecutionContext,
      ApplicantProgramBlockEditViewFactory editViewFactory,
      FormFactory formFactory,
      StorageClient storageClient,
      StoredFileRepository storedFileRepository,
      ProfileUtils profileUtils,
      Config configuration,
      FileUploadViewStrategy fileUploadViewStrategy) {
    this.applicantService = checkNotNull(applicantService);
    this.messagesApi = checkNotNull(messagesApi);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.formFactory = checkNotNull(formFactory);
    this.storageClient = checkNotNull(storageClient);
    this.storedFileRepository = checkNotNull(storedFileRepository);
    this.profileUtils = checkNotNull(profileUtils);
    this.baseUrl = checkNotNull(configuration).getString("base_url");
    this.editView =
        editViewFactory.create(new ApplicantQuestionRendererFactory(fileUploadViewStrategy));
  }

  /**
   * This method renders all questions in the block of the program and presents to the applicant.
   *
   * <p>The difference between `edit` and `review` is the next block the applicant will see after
   * submitting the answers.
   *
   * <p>`edit` takes the applicant to the next in-progress block, see {@link
   * ReadOnlyApplicantProgramService#getInProgressBlocks()}. If there are no more blocks, summary
   * page is shown.
   */
  @Secure
  public CompletionStage<Result> edit(
      Request request, long applicantId, long programId, String blockId) {
    return editOrReview(request, applicantId, programId, blockId, false);
  }

  /**
   * This method renders all questions in the block of the program and presents to the applicant.
   *
   * <p>The difference between `edit` and `review` is the next block the applicant will see after
   * submitting the answers.
   *
   * <p>`review` takes the applicant to the first incomplete block. If there are no more blocks,
   * summary page is shown.
   */
  @Secure
  public CompletionStage<Result> review(
      Request request, long applicantId, long programId, String blockId) {
    return editOrReview(request, applicantId, programId, blockId, true);
  }

  /** This method navigates to the previous page of the application. */
  @Secure
  public CompletionStage<Result> previous(
      Request request, long applicantId, long programId, int previousBlockIndex, boolean inReview) {
    CompletionStage<Optional<String>> applicantStage = this.applicantService.getName(applicantId);

    CompletableFuture<Void> applicantAuthCompletableFuture =
        applicantStage
            .thenComposeAsync(
                v -> checkApplicantAuthorization(profileUtils, request, applicantId),
                httpExecutionContext.current())
            .toCompletableFuture();

    CompletableFuture<ReadOnlyApplicantProgramService> applicantProgramServiceCompletableFuture =
        applicantStage
            .thenComposeAsync(
                v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
                httpExecutionContext.current())
            .toCompletableFuture();

    return CompletableFuture.allOf(
            applicantAuthCompletableFuture, applicantProgramServiceCompletableFuture)
        .thenApplyAsync(
            (v) -> {
              ReadOnlyApplicantProgramService roApplicantProgramService =
                  applicantProgramServiceCompletableFuture.join();
              ImmutableList<Block> blocks = roApplicantProgramService.getAllActiveBlocks();
              String blockId = blocks.get(previousBlockIndex).getId();
              Optional<Block> block = roApplicantProgramService.getBlock(blockId);

              if (block.isPresent()) {
                Optional<String> applicantName = applicantStage.toCompletableFuture().join();
                return ok(
                    editView.render(
                        buildApplicationBaseViewParams(
                            request,
                            applicantId,
                            programId,
                            blockId,
                            inReview,
                            roApplicantProgramService,
                            block.get(),
                            applicantName,
                            ApplicantQuestionRendererParams.ErrorDisplayMode.HIDE_ERRORS)));
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
  private CompletionStage<Result> editOrReview(
      Request request, long applicantId, long programId, String blockId, boolean inReview) {
    CompletionStage<Optional<String>> applicantStage = this.applicantService.getName(applicantId);

    return applicantStage
        .thenComposeAsync(
            v -> checkApplicantAuthorization(profileUtils, request, applicantId),
            httpExecutionContext.current())
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            httpExecutionContext.current())
        .thenApplyAsync(
            (roApplicantProgramService) -> {
              Optional<Block> block = roApplicantProgramService.getBlock(blockId);

              if (block.isPresent()) {
                Optional<String> applicantName = applicantStage.toCompletableFuture().join();
                return ok(
                    editView.render(
                        buildApplicationBaseViewParams(
                            request,
                            applicantId,
                            programId,
                            blockId,
                            inReview,
                            roApplicantProgramService,
                            block.get(),
                            applicantName,
                            ApplicantQuestionRendererParams.ErrorDisplayMode.HIDE_ERRORS)));
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

  /**
   * This method is used by the file upload question. We let users directly upload files to S3
   * bucket from browsers. On success, users are redirected to this method. The redirect is a GET
   * method with file key in the query string. We parse and store them in the database for record
   * and redirect users to the next block or review page.
   */
  @Secure
  public CompletionStage<Result> updateFile(
      Request request, long applicantId, long programId, String blockId, boolean inReview) {
    CompletionStage<Optional<String>> applicantStage = this.applicantService.getName(applicantId);

    return applicantStage
        .thenComposeAsync(
            v -> checkApplicantAuthorization(profileUtils, request, applicantId),
            httpExecutionContext.current())
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            httpExecutionContext.current())
        .thenComposeAsync(
            (roApplicantProgramService) -> {
              Optional<Block> block = roApplicantProgramService.getBlock(blockId);

              if (!block.isPresent() || !block.get().isFileUpload()) {
                return failedFuture(new ProgramBlockNotFoundException(programId, blockId));
              }

              Optional<String> bucket = request.queryString("bucket");
              Optional<String> key = request.queryString("key");

              // Original file name is only set for Azure, where we have to generate a UUID when
              // uploading a file to Azure Blob storage because we cannot upload a file without a
              // name. For AWS, the file key and original file name are the same. For the future,
              // GCS supports POST uploads so this field won't be needed either:
              // <link> https://cloud.google.com/storage/docs/xml-api/post-object-forms </link>
              // This is only really needed for Azure blob storage.
              Optional<String> originalFileName = request.queryString("originalFileName");

              if (!bucket.isPresent() || !key.isPresent()) {
                return failedFuture(
                    new IllegalArgumentException("missing file key and bucket names"));
              }

              FileUploadQuestion fileUploadQuestion =
                  block.get().getQuestions().stream()
                      .filter(question -> question.getType().equals(QuestionType.FILEUPLOAD))
                      .findAny()
                      .get()
                      .createFileUploadQuestion();

              ImmutableMap.Builder<String, String> fileUploadQuestionFormData =
                  new ImmutableMap.Builder<>();
              fileUploadQuestionFormData.put(
                  fileUploadQuestion.getFileKeyPath().toString(), key.get());
              if (originalFileName.isPresent()) {
                fileUploadQuestionFormData.put(
                    fileUploadQuestion.getOriginalFileNamePath().toString(),
                    originalFileName.get());
              }

              return ensureFileRecord(key.get(), originalFileName)
                  .thenComposeAsync(
                      (StoredFile unused) ->
                          applicantService.stageAndUpdateIfValid(
                              applicantId, programId, blockId, fileUploadQuestionFormData.build()));
            },
            httpExecutionContext.current())
        .thenComposeAsync(
            (roApplicantProgramService) ->
                renderErrorOrRedirectToNextBlock(
                    request,
                    applicantId,
                    programId,
                    blockId,
                    applicantStage.toCompletableFuture().join(),
                    inReview,
                    roApplicantProgramService),
            httpExecutionContext.current())
        .exceptionally(ex -> handleUpdateExceptions(ex));
  }

  @Secure
  public CompletionStage<Result> update(
      Request request, long applicantId, long programId, String blockId, boolean inReview) {
    CompletionStage<Optional<String>> applicantStage = this.applicantService.getName(applicantId);

    return applicantStage
        .thenComposeAsync(
            v -> checkApplicantAuthorization(profileUtils, request, applicantId),
            httpExecutionContext.current())
        .thenComposeAsync(
            v -> {
              DynamicForm form = formFactory.form().bindFromRequest(request);
              ImmutableMap<String, String> formData = cleanForm(form.rawData());

              return applicantService.stageAndUpdateIfValid(
                  applicantId, programId, blockId, formData);
            },
            httpExecutionContext.current())
        .thenComposeAsync(
            roApplicantProgramService ->
                renderErrorOrRedirectToNextBlock(
                    request,
                    applicantId,
                    programId,
                    blockId,
                    applicantStage.toCompletableFuture().join(),
                    inReview,
                    roApplicantProgramService),
            httpExecutionContext.current())
        .exceptionally(ex -> handleUpdateExceptions(ex));
  }

  private CompletionStage<Result> renderErrorOrRedirectToNextBlock(
      Request request,
      long applicantId,
      long programId,
      String blockId,
      Optional<String> applicantName,
      boolean inReview,
      ReadOnlyApplicantProgramService roApplicantProgramService) {
    Optional<Block> thisBlockUpdatedMaybe = roApplicantProgramService.getBlock(blockId);
    if (thisBlockUpdatedMaybe.isEmpty()) {
      return failedFuture(new ProgramBlockNotFoundException(programId, blockId));
    }
    Block thisBlockUpdated = thisBlockUpdatedMaybe.get();

    // Validation errors: re-render this block with errors and previously entered data.
    if (thisBlockUpdated.hasErrors()
        || thisBlockUpdated.hasRequiredQuestionsThatAreSkippedInCurrentProgram()) {
      return supplyAsync(
          () ->
              ok(
                  editView.render(
                      buildApplicationBaseViewParams(
                          request,
                          applicantId,
                          programId,
                          blockId,
                          inReview,
                          roApplicantProgramService,
                          thisBlockUpdated,
                          applicantName,
                          ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS))));
    }

    if (inReview) {
      Optional<String> nextBlockIdMaybe =
          roApplicantProgramService.getFirstIncompleteBlockExcludingStatic().map(Block::getId);
      return nextBlockIdMaybe.isEmpty()
          ? supplyAsync(
              () ->
                  redirect(routes.ApplicantProgramReviewController.review(applicantId, programId)))
          : supplyAsync(
              () ->
                  redirect(
                      routes.ApplicantProgramBlocksController.review(
                          applicantId, programId, nextBlockIdMaybe.get())));
    } else {
      Optional<String> nextBlockIdMaybe =
          roApplicantProgramService.getInProgressBlockAfter(blockId).map(Block::getId);
      return nextBlockIdMaybe.isEmpty()
          ? supplyAsync(
              () ->
                  redirect(routes.ApplicantProgramReviewController.review(applicantId, programId)))
          : supplyAsync(
              () ->
                  redirect(
                      routes.ApplicantProgramBlocksController.edit(
                          applicantId, programId, nextBlockIdMaybe.get())));
    }
  }

  private ImmutableMap<String, String> cleanForm(Map<String, String> formData) {
    return formData.entrySet().stream()
        .filter(entry -> !STRIPPED_FORM_FIELDS.contains(entry.getKey()))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private ApplicationBaseView.Params buildApplicationBaseViewParams(
      Request request,
      long applicantId,
      long programId,
      String blockId,
      boolean inReview,
      ReadOnlyApplicantProgramService roApplicantProgramService,
      Block block,
      Optional<String> applicantName,
      ApplicantQuestionRendererParams.ErrorDisplayMode errorDisplayMode) {
    return ApplicationBaseView.Params.builder()
        .setRequest(request)
        .setMessages(messagesApi.preferred(request))
        .setApplicantId(applicantId)
        .setProgramTitle(roApplicantProgramService.getProgramTitle())
        .setProgramId(programId)
        .setBlock(block)
        .setInReview(inReview)
        .setBlockIndex(roApplicantProgramService.getBlockIndex(blockId))
        .setTotalBlockCount(roApplicantProgramService.getAllActiveBlocks().size())
        .setApplicantName(applicantName)
        .setPreferredLanguageSupported(roApplicantProgramService.preferredLanguageSupported())
        .setStorageClient(storageClient)
        .setBaseUrl(baseUrl)
        .setErrorDisplayMode(errorDisplayMode)
        .build();
  }

  private CompletionStage<StoredFile> ensureFileRecord(
      String key, Optional<String> originalFileName) {
    return storedFileRepository
        .lookupFile(key)
        .thenComposeAsync(
            (Optional<StoredFile> maybeStoredFile) -> {
              // If there is already a stored file with this key in the database, then
              // the applicant has uploaded a file with the same name for the same
              // block and question, overwriting the original in file storage.
              if (maybeStoredFile.isPresent()) {
                return completedFuture(maybeStoredFile.get());
              }

              var storedFile = new StoredFile();
              storedFile.setName(key);
              originalFileName.ifPresent(storedFile::setOriginalFileName);

              return storedFileRepository.insert(storedFile);
            },
            httpExecutionContext.current());
  }

  private Result handleUpdateExceptions(Throwable throwable) {
    if (throwable instanceof CompletionException) {
      Throwable cause = throwable.getCause();
      if (cause instanceof SecurityException) {
        return unauthorized();
      } else if (cause instanceof ApplicantNotFoundException
          || cause instanceof IllegalArgumentException
          || cause instanceof PathNotInBlockException
          || cause instanceof ProgramBlockNotFoundException
          || cause instanceof ProgramNotFoundException
          || cause instanceof UnsupportedScalarTypeException) {
        logger.error("Exception while updating applicant data", cause);
        return badRequest("Unable to process this request");
      }
      throw new RuntimeException(cause);
    }
    throw new RuntimeException(throwable);
  }
}
