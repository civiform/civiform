package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import controllers.CiviFormController;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.StoredFileModel;
import org.pac4j.play.java.Secure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parsers.applicant.ApplicantStreamingMultipartBodyParser;
import play.data.FormFactory;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.StoredFileRepository;
import repository.VersionRepository;
import services.applicant.ApplicantService;
import services.applicant.Block;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applicant.exception.ApplicantNotFoundException;
import services.applicant.exception.ProgramBlockNotFoundException;
import services.applicant.question.FileUploadQuestion;
import services.program.PathNotInBlockException;
import services.program.ProgramNotFoundException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.exceptions.UnsupportedScalarTypeException;
import services.settings.SettingsManifest;
import views.questiontypes.FileUploadQuestionPartialView;

/** File upload endpoints */
public final class FileUploadController extends CiviFormController {

  private final ApplicantService applicantService;
  private final ClassLoaderExecutionContext classLoaderExecutionContext;
  private final FormFactory formFactory;
  private final StoredFileRepository storedFileRepository;
  private final SettingsManifest settingsManifest;
  private final FileUploadQuestionPartialView fileUploadQuestionPartialView;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject
  public FileUploadController(
      ApplicantService applicantService,
      ClassLoaderExecutionContext classLoaderExecutionContext,
      FormFactory formFactory,
      StoredFileRepository storedFileRepository,
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      SettingsManifest settingsManifest,
      FileUploadQuestionPartialView fileUploadQuestionPartialView) {
    super(profileUtils, versionRepository);
    this.applicantService = checkNotNull(applicantService);
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.formFactory = checkNotNull(formFactory);
    this.storedFileRepository = checkNotNull(storedFileRepository);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.fileUploadQuestionPartialView = checkNotNull(fileUploadQuestionPartialView);
  }

  @Secure
  @BodyParser.Of(ApplicantStreamingMultipartBodyParser.class)
  public CompletionStage<Result> hxSelectFileForUpload(
      Request request, long programId, String blockId, long questionId) {
    if (!settingsManifest.getFileUploadQuestionImprovementsEnabled(request)) {
      return CompletableFuture.completedFuture(notFound());
    }

    Optional<Long> optionalApplicantId = getApplicantId(request);
    if (optionalApplicantId.isEmpty()) {
      return CompletableFuture.completedFuture(badRequest());
    }

    long applicantId = optionalApplicantId.get();

    // The body has already been streamed to cloud storage by
    // ApplicantStreamingMultipartBodyParser. Each FilePart's ref carries the generated fileKey.
    Http.MultipartFormData<String> body = request.body().asMultipartFormData();
    if (body == null) {
      return CompletableFuture.completedFuture(badRequest());
    }

    Http.MultipartFormData.FilePart<String> filePart = body.getFile("file");
    if (filePart == null) {
      return CompletableFuture.completedFuture(badRequest());
    }

    long parsedQuestionId = questionId;

    String originalFileName = filePart.getFilename();
    String fileKey = filePart.getRef();

    return checkApplicantAuthorization(request, applicantId)
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            classLoaderExecutionContext.current())
        .thenComposeAsync(
            roApplicantProgramService -> {
              Optional<Block> block = roApplicantProgramService.getActiveBlock(blockId);

              if (block.isEmpty() || !block.get().isFileUpload()) {
                return failedFuture(new ProgramBlockNotFoundException(programId, blockId));
              }

              final FileUploadQuestion fileUploadQuestion;
              try {
                fileUploadQuestion = block.get().findFileUploadQuestion(parsedQuestionId);
              } catch (QuestionNotFoundException e) {
                return failedFuture(e);
              }

              if (!fileUploadQuestion.canUploadFile()) {
                return failedFuture(
                    new IllegalArgumentException(
                        String.format(
                            "Cannot upload additional files for question"
                                + " %s, in program %s, block %s, for"
                                + " applicant %s.",
                            fileUploadQuestion
                                .getApplicantQuestion()
                                .getQuestionDefinition()
                                .getId(),
                            programId,
                            blockId,
                            applicantId)));
              }

              ImmutableMap<String, String> formData =
                  fileUploadQuestion.buildFormDataForAdd(fileKey, originalFileName);

              return getOrMakeFileRecord(fileKey, Optional.of(originalFileName), applicantId)
                  .thenComposeAsync(
                      storedFile ->
                          applicantService.stageAndUpdateIfValid(
                              applicantId,
                              programId,
                              blockId,
                              formData,
                              settingsManifest.getEsriAddressServiceAreaValidationEnabled(request),
                              /* forceUpdate= */ true,
                              settingsManifest.getApiBridgeEnabled(request)));
            },
            classLoaderExecutionContext.current())
        .thenComposeAsync(
            roApplicantProgramService ->
                renderFileUploadPartial(
                    request, programId, blockId, parsedQuestionId, roApplicantProgramService),
            classLoaderExecutionContext.current())
        .exceptionally(this::handleUpdateExceptions);
  }

  /**
   * HTMX endpoint that removes a previously-uploaded file from the applicant's answer for the file
   * upload question in {@code blockId}. Returns an HTML partial with OOB swaps to update the file
   * list and hidden inputs.
   */
  @Secure
  public CompletionStage<Result> hxRemoveFile(
      Request request, long programId, String blockId, long questionId) {
    if (!settingsManifest.getFileUploadQuestionImprovementsEnabled(request)) {
      return CompletableFuture.completedFuture(notFound());
    }

    Optional<Long> optionalApplicantId = getApplicantId(request);
    if (optionalApplicantId.isEmpty()) {
      return CompletableFuture.completedFuture(badRequest());
    }

    long applicantId = optionalApplicantId.get();

    long parsedQuestionId = questionId;

    String fileKey = formFactory.form().bindFromRequest(request).get("fileKey");
    if (fileKey == null || fileKey.isBlank()) {
      return CompletableFuture.completedFuture(badRequest());
    }

    return checkApplicantAuthorization(request, applicantId)
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            classLoaderExecutionContext.current())
        .thenComposeAsync(
            roApplicantProgramService -> {
              Optional<Block> block = roApplicantProgramService.getActiveBlock(blockId);

              if (block.isEmpty() || !block.get().isFileUpload()) {
                return failedFuture(new ProgramBlockNotFoundException(programId, blockId));
              }

              final FileUploadQuestion fileUploadQuestion;
              try {
                fileUploadQuestion = block.get().findFileUploadQuestion(parsedQuestionId);
              } catch (QuestionNotFoundException e) {
                return failedFuture(e);
              }

              ImmutableMap<String, String> formData =
                  fileUploadQuestion.buildFormDataForRemove(fileKey);

              return applicantService.stageAndUpdateIfValid(
                  applicantId,
                  programId,
                  blockId,
                  formData,
                  settingsManifest.getEsriAddressServiceAreaValidationEnabled(request),
                  /* forceUpdate= */ true,
                  settingsManifest.getApiBridgeEnabled(request));
            },
            classLoaderExecutionContext.current())
        .thenComposeAsync(
            roApplicantProgramService ->
                renderFileUploadPartial(
                    request, programId, blockId, parsedQuestionId, roApplicantProgramService),
            classLoaderExecutionContext.current())
        .exceptionally(this::handleUpdateExceptions);
  }

  /**
   * Renders the OOB partial for the file upload question identified by {@code questionId}, reading
   * from the supplied (post-stage) {@link ReadOnlyApplicantProgramService}. Shared by {@link
   * #hxSelectFileForUpload} and {@link #hxRemoveFile}.
   */
  private CompletionStage<Result> renderFileUploadPartial(
      Request request,
      long programId,
      String blockId,
      long questionId,
      ReadOnlyApplicantProgramService roApplicantProgramService) {
    return CompletableFuture.supplyAsync(
            () ->
                fileUploadQuestionPartialView.renderHtmxSuccess(
                    request, programId, blockId, questionId, roApplicantProgramService),
            classLoaderExecutionContext.current())
        .exceptionallyAsync(ex -> internalServerError(), classLoaderExecutionContext.current());
  }

  private CompletionStage<StoredFileModel> getOrMakeFileRecord(
      String key, Optional<String> originalFileName, long applicantId) {
    return storedFileRepository
        .lookupFile(key)
        .thenComposeAsync(
            (Optional<StoredFileModel> maybeStoredFile) -> {
              // If there is already a stored file with this key in the database, then
              // the applicant has uploaded a file with the same name for the same
              // block and question, overwriting the original in file storage.
              if (maybeStoredFile.isPresent()) {
                return completedFuture(maybeStoredFile.get());
              }

              var storedFile = new StoredFileModel();
              storedFile.setName(key);
              storedFile.getAcls().addApplicantToReaders(applicantId);
              originalFileName.ifPresent(storedFile::setOriginalFileName);

              return storedFileRepository.insert(storedFile);
            },
            classLoaderExecutionContext.current());
  }

  private Result handleUpdateExceptions(Throwable throwable) {
    if (throwable instanceof CompletionException) {
      Throwable cause = throwable.getCause();
      if (cause instanceof SecurityException) {
        return unauthorized();
      } else if (cause instanceof ProgramNotFoundException) {
        logger.error("Program not found", cause);
        return badRequest("Program not found");
      } else if (cause instanceof ApplicantNotFoundException
          || cause instanceof IllegalArgumentException
          || cause instanceof PathNotInBlockException
          || cause instanceof ProgramBlockNotFoundException
          || cause instanceof ProgramNotFoundException
          || cause instanceof QuestionNotFoundException
          || cause instanceof UnsupportedScalarTypeException) {
        logger.error("Exception while updating applicant data", cause);
        return badRequest("Unable to process this request");
      }
      throw new RuntimeException(cause);
    }
    throw new RuntimeException(throwable);
  }
}
