package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS;
import static views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS_WITH_MODAL_REVIEW;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import controllers.CiviFormController;
import controllers.geo.AddressSuggestionJsonSerializer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.StoredFileModel;
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
import repository.VersionRepository;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.applicant.Block;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applicant.exception.ApplicantNotFoundException;
import services.applicant.exception.ProgramBlockNotFoundException;
import services.applicant.question.AddressQuestion;
import services.applicant.question.FileUploadQuestion;
import services.cloud.ApplicantStorageClient;
import services.geo.AddressSuggestion;
import services.geo.AddressSuggestionGroup;
import services.program.PathNotInBlockException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.exceptions.UnsupportedScalarTypeException;
import services.question.types.QuestionType;
import services.settings.SettingsManifest;
import views.ApplicationBaseView;
import views.applicant.AddressCorrectionBlockView;
import views.applicant.ApplicantFileUploadRenderer;
import views.applicant.ApplicantProgramBlockEditView;
import views.applicant.ApplicantProgramBlockEditViewFactory;
import views.applicant.IneligibleBlockView;
import views.components.ToastMessage;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;

/**
 * Controller for handling an applicant filling out a single program. CAUTION: you must explicitly
 * check the current profile so that an unauthorized user cannot access another applicant's data!
 */
public final class ApplicantProgramBlocksController extends CiviFormController {
  private static final ImmutableSet<String> STRIPPED_FORM_FIELDS = ImmutableSet.of("csrfToken");
  private static final String ADDRESS_JSON_SESSION_KEY = "addressJson";

  private final ApplicantService applicantService;
  private final MessagesApi messagesApi;
  private final HttpExecutionContext httpExecutionContext;
  private final ApplicantProgramBlockEditView editView;
  private final FormFactory formFactory;
  private final ApplicantStorageClient applicantStorageClient;
  private final StoredFileRepository storedFileRepository;
  private final SettingsManifest settingsManifest;
  private final String baseUrl;
  private final IneligibleBlockView ineligibleBlockView;
  private final AddressCorrectionBlockView addressCorrectionBlockView;
  private final AddressSuggestionJsonSerializer addressSuggestionJsonSerializer;
  private final ProgramService programService;
  private final ApplicantRoutes applicantRoutes;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject
  public ApplicantProgramBlocksController(
      ApplicantService applicantService,
      MessagesApi messagesApi,
      HttpExecutionContext httpExecutionContext,
      ApplicantProgramBlockEditViewFactory editViewFactory,
      FormFactory formFactory,
      ApplicantStorageClient applicantStorageClient,
      StoredFileRepository storedFileRepository,
      ProfileUtils profileUtils,
      Config configuration,
      SettingsManifest settingsManifest,
      ApplicantFileUploadRenderer applicantFileUploadRenderer,
      IneligibleBlockView ineligibleBlockView,
      AddressCorrectionBlockView addressCorrectionBlockView,
      AddressSuggestionJsonSerializer addressSuggestionJsonSerializer,
      ProgramService programService,
      VersionRepository versionRepository,
      ApplicantRoutes applicantRoutes) {
    super(profileUtils, versionRepository);
    this.applicantService = checkNotNull(applicantService);
    this.messagesApi = checkNotNull(messagesApi);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.formFactory = checkNotNull(formFactory);
    this.applicantStorageClient = checkNotNull(applicantStorageClient);
    this.storedFileRepository = checkNotNull(storedFileRepository);
    this.baseUrl = checkNotNull(configuration).getString("base_url");
    this.settingsManifest = checkNotNull(settingsManifest);
    this.ineligibleBlockView = checkNotNull(ineligibleBlockView);
    this.addressCorrectionBlockView = checkNotNull(addressCorrectionBlockView);
    this.addressSuggestionJsonSerializer = checkNotNull(addressSuggestionJsonSerializer);
    this.applicantRoutes = checkNotNull(applicantRoutes);
    this.editView =
        editViewFactory.create(new ApplicantQuestionRendererFactory(applicantFileUploadRenderer));
    this.programService = checkNotNull(programService);
  }

  /**
   * Renders all questions in the block of the program and presents to the applicant.
   *
   * <p>The difference between `edit` and `review` is the next block the applicant will see after
   * submitting the answers.
   *
   * <p>`edit` takes the applicant to the next in-progress block, see {@link
   * ReadOnlyApplicantProgramService#getInProgressBlocks()}. If there are no more blocks, summary
   * page is shown.
   *
   * <p>`questionName` is present when answering a question or reviewing an answer.
   */
  @Secure
  public CompletionStage<Result> editWithApplicantId(
      Request request,
      long applicantId,
      long programId,
      String blockId,
      Optional<String> questionName) {
    return editOrReview(request, applicantId, programId, blockId, false, questionName);
  }

  /**
   * Renders all questions in the block of the program and presents to the applicant.
   *
   * <p>The difference between `edit` and `review` is the next block the applicant will see after
   * submitting the answers.
   *
   * <p>`edit` takes the applicant to the next in-progress block, see {@link
   * ReadOnlyApplicantProgramService#getInProgressBlocks()}. If there are no more blocks, summary
   * page is shown.
   *
   * <p>`questionName` is present when answering a question or reviewing an answer.
   */
  @Secure
  public CompletionStage<Result> edit(
      Request request, long programId, String blockId, Optional<String> questionName) {
    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return CompletableFuture.completedFuture(redirectToHome());
    }
    return editWithApplicantId(
        request, applicantId.orElseThrow(), programId, blockId, questionName);
  }

  /**
   * Renders all questions in the block of the program and presents to the applicant.
   *
   * <p>The difference between `edit` and `review` is the next block the applicant will see after
   * submitting the answers.
   *
   * <p>`review` takes the applicant to the first incomplete block. If there are no more blocks,
   * summary page is shown.
   *
   * <p>`questionName` is present when answering a question or reviewing an answer.
   */
  @Secure
  public CompletionStage<Result> reviewWithApplicantId(
      Request request,
      long applicantId,
      long programId,
      String blockId,
      Optional<String> questionName) {
    return editOrReview(request, applicantId, programId, blockId, true, questionName);
  }

  /**
   * Renders all questions in the block of the program and presents to the applicant.
   *
   * <p>The difference between `edit` and `review` is the next block the applicant will see after
   * submitting the answers.
   *
   * <p>`review` takes the applicant to the first incomplete block. If there are no more blocks,
   * summary page is shown.
   *
   * <p>`questionName` is present when answering a question or reviewing an answer.
   */
  @Secure
  public CompletionStage<Result> review(
      Request request, long programId, String blockId, Optional<String> questionName) {
    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return CompletableFuture.completedFuture(redirectToHome());
    }
    return reviewWithApplicantId(
        request, applicantId.orElseThrow(), programId, blockId, questionName);
  }

  /** Handles the applicant's selection from the address correction options. */
  @Secure
  public CompletionStage<Result> confirmAddressWithApplicantId(
      Request request, long applicantId, long programId, String blockId, boolean inReview) {

    DynamicForm form = formFactory.form().bindFromRequest(request);
    Optional<String> selectedAddress =
        Optional.ofNullable(form.get(AddressCorrectionBlockView.SELECTED_ADDRESS_NAME));
    Optional<String> maybeAddressJson = request.session().get(ADDRESS_JSON_SESSION_KEY);

    ImmutableList<AddressSuggestion> suggestions =
        addressSuggestionJsonSerializer.deserialize(
            maybeAddressJson.orElseThrow(() -> new RuntimeException("Address JSON missing")));
    return confirmAddressWithSuggestions(
        request, applicantId, programId, blockId, inReview, selectedAddress, suggestions);
  }

  /** Handles the applicant's selection from the address correction options. */
  @Secure
  public CompletionStage<Result> confirmAddress(
      Request request, long programId, String blockId, boolean inReview) {
    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return CompletableFuture.completedFuture(redirectToHome());
    }
    return confirmAddressWithApplicantId(
        request, applicantId.orElseThrow(), programId, blockId, inReview);
  }

  /** Saves the selected corrected address to the db and redirects the user to the next screen */
  private CompletionStage<Result> confirmAddressWithSuggestions(
      Request request,
      long applicantId,
      long programId,
      String blockId,
      boolean inReview,
      Optional<String> selectedAddress,
      ImmutableList<AddressSuggestion> suggestions) {
    CompletableFuture<ApplicantPersonalInfo> applicantStage =
        applicantService.getPersonalInfo(applicantId).toCompletableFuture();

    return CompletableFuture.allOf(
            checkApplicantAuthorization(request, applicantId), applicantStage)
        .thenComposeAsync(
            v ->
                applicantService.getCorrectedAddress(
                    applicantId, programId, blockId, selectedAddress, suggestions),
            httpExecutionContext.current())
        .thenComposeAsync(
            questionPathToValueMap ->
                applicantService.stageAndUpdateIfValid(
                    applicantId,
                    programId,
                    blockId,
                    cleanForm(questionPathToValueMap),
                    settingsManifest.getEsriAddressServiceAreaValidationEnabled(request)),
            httpExecutionContext.current())
        .thenComposeAsync(
            roApplicantProgramService -> {
              removeAddressJsonFromSession(request);
              CiviFormProfile profile = profileUtils.currentUserProfileOrThrow(request);
              return renderErrorOrRedirectToNextBlock(
                  request,
                  profile,
                  applicantId,
                  programId,
                  blockId,
                  applicantStage.join(),
                  inReview,
                  // TODO(#6450): Pass in the applicant-requested action to direct them correctly.
                  ApplicantRequestedAction.NEXT_BLOCK,
                  roApplicantProgramService);
            },
            httpExecutionContext.current())
        .exceptionally(
            throwable -> {
              removeAddressJsonFromSession(request);
              return handleUpdateExceptions(throwable);
            });
  }

  /**
   * Clean up the address suggestions json from the session. Remove this to prevent the chance of
   * the old session value being used on subsequent address corrections.
   */
  private void removeAddressJsonFromSession(Request request) {
    request.session().removing(ADDRESS_JSON_SESSION_KEY);
  }

  /** Navigates to the previous page of the application. */
  @Secure
  public CompletionStage<Result> previousWithApplicantId(
      Request request, long applicantId, long programId, int previousBlockIndex, boolean inReview) {
    CompletionStage<ApplicantPersonalInfo> applicantStage =
        this.applicantService.getPersonalInfo(applicantId);

    CompletableFuture<Void> applicantAuthCompletableFuture =
        applicantStage
            .thenComposeAsync(
                v -> checkApplicantAuthorization(request, applicantId),
                httpExecutionContext.current())
            .toCompletableFuture();

    CiviFormProfile profile = profileUtils.currentUserProfileOrThrow(request);

    CompletableFuture<ReadOnlyApplicantProgramService> applicantProgramServiceCompletableFuture =
        applicantStage
            .thenComposeAsync(v -> checkProgramAuthorization(request, programId))
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
              Optional<Block> block = roApplicantProgramService.getActiveBlock(blockId);

              if (block.isPresent()) {
                ApplicantPersonalInfo personalInfo = applicantStage.toCompletableFuture().join();
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
                            personalInfo,
                            ApplicantQuestionRendererParams.ErrorDisplayMode.HIDE_ERRORS,
                            applicantRoutes,
                            profile)));
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

  /** Navigates to the previous page of the application. */
  @Secure
  public CompletionStage<Result> previous(
      Request request, long programId, int previousBlockIndex, boolean inReview) {
    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return CompletableFuture.completedFuture(redirectToHome());
    }
    return previousWithApplicantId(
        request, applicantId.orElseThrow(), programId, previousBlockIndex, inReview);
  }

  @Secure
  private CompletionStage<Result> editOrReview(
      Request request,
      long applicantId,
      long programId,
      String blockId,
      boolean inReview,
      Optional<String> questionName) {
    CompletionStage<ApplicantPersonalInfo> applicantStage =
        this.applicantService.getPersonalInfo(applicantId);

    Optional<ToastMessage> flashSuccessBanner =
        request.flash().get("success-banner").map(m -> ToastMessage.success(m));

    return applicantStage
        .thenComposeAsync(
            v -> checkApplicantAuthorization(request, applicantId), httpExecutionContext.current())
        .thenComposeAsync(v -> checkProgramAuthorization(request, programId))
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            httpExecutionContext.current())
        .thenApplyAsync(
            (roApplicantProgramService) -> {
              Optional<Block> block = roApplicantProgramService.getActiveBlock(blockId);

              if (block.isPresent()) {
                ApplicantPersonalInfo personalInfo = applicantStage.toCompletableFuture().join();
                CiviFormProfile profile = profileUtils.currentUserProfileOrThrow(request);
                return ok(
                    editView.render(
                        applicationBaseViewParamsBuilder(
                                request,
                                applicantId,
                                programId,
                                blockId,
                                inReview,
                                roApplicantProgramService,
                                block.get(),
                                personalInfo,
                                ApplicantQuestionRendererParams.ErrorDisplayMode.HIDE_ERRORS,
                                questionName,
                                applicantRoutes,
                                profile)
                            .setBannerMessage(flashSuccessBanner)
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

  /**
   * Used by the file upload question. We let users directly upload files to S3 bucket from
   * browsers. On success, users are redirected to this method. The redirect is a GET method with
   * file key in the query string. We parse and store them in the database for record and redirect
   * users to the next block or review page.
   */
  @Secure
  public CompletionStage<Result> updateFileWithApplicantId(
      Request request, long applicantId, long programId, String blockId, boolean inReview) {
    CompletionStage<ApplicantPersonalInfo> applicantStage =
        this.applicantService.getPersonalInfo(applicantId);

    return applicantStage
        .thenComposeAsync(
            v -> checkApplicantAuthorization(request, applicantId), httpExecutionContext.current())
        .thenComposeAsync(v -> checkProgramAuthorization(request, programId))
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            httpExecutionContext.current())
        .thenComposeAsync(
            (roApplicantProgramService) -> {
              Optional<Block> block = roApplicantProgramService.getActiveBlock(blockId);

              if (block.isEmpty() || !block.get().isFileUpload()) {
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

              if (bucket.isEmpty() || key.isEmpty()) {
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
              originalFileName.ifPresent(
                  s ->
                      fileUploadQuestionFormData.put(
                          fileUploadQuestion.getOriginalFileNamePath().toString(), s));

              return ensureFileRecord(key.get(), originalFileName)
                  .thenComposeAsync(
                      (StoredFileModel unused) ->
                          applicantService.stageAndUpdateIfValid(
                              applicantId,
                              programId,
                              blockId,
                              fileUploadQuestionFormData.build(),
                              settingsManifest.getEsriAddressServiceAreaValidationEnabled(
                                  request)));
            },
            httpExecutionContext.current())
        .thenComposeAsync(
            (roApplicantProgramService) -> {
              CiviFormProfile profile = profileUtils.currentUserProfileOrThrow(request);
              return renderErrorOrRedirectToNextBlock(
                  request,
                  profile,
                  applicantId,
                  programId,
                  blockId,
                  applicantStage.toCompletableFuture().join(),
                  inReview,
                  // TODO(#6450): Pass in the applicant-requested action to direct them correctly.
                  ApplicantRequestedAction.NEXT_BLOCK,
                  roApplicantProgramService);
            },
            httpExecutionContext.current())
        .exceptionally(this::handleUpdateExceptions);
  }

  /**
   * Used by the file upload question. We let users directly upload files to S3 bucket from
   * browsers. On success, users are redirected to this method. The redirect is a GET method with
   * file key in the query string. We parse and store them in the database for record and redirect
   * users to the next block or review page.
   */
  @Secure
  public CompletionStage<Result> updateFile(
      Request request, long programId, String blockId, boolean inReview) {
    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return CompletableFuture.completedFuture(redirectToHome());
    }
    return updateFileWithApplicantId(
        request, applicantId.orElseThrow(), programId, blockId, inReview);
  }

  /**
   * Accepts, validates and saves submission of applicant data for {@code blockId}.
   *
   * <p>Returns the applicable next step in the flow:
   *
   * <ul>
   *   <li>If there are errors, then the edit page for the same block with the errors is shown.
   *   <li>If {@code applicantRequestedAction} is the {@link ApplicantRequestedAction#REVIEW_PAGE},
   *       then the review page is shown.
   *   <li>If {@code applicantRequestedAction} is the {@link ApplicantRequestedAction#NEXT_BLOCK},
   *       then we use {@code inReview} to determine what block to show next:
   *       <ul>
   *         <li>If {@code inReview} then the next incomplete block is shown.
   *         <li>If not {@code inReview} the next visible block is shown.
   *         <li>If there is no next block the program review page is shown.
   *       </ul>
   * </ul>
   *
   * @param applicantRequestedAction should match the name of one of the values in {@link
   *     ApplicantRequestedAction}.
   */
  @Secure
  public CompletionStage<Result> updateWithApplicantId(
      Request request,
      long applicantId,
      long programId,
      String blockId,
      boolean inReview,
      String applicantRequestedAction) {
    CompletionStage<ApplicantPersonalInfo> applicantStage =
        this.applicantService.getPersonalInfo(applicantId);

    return applicantStage
        .thenComposeAsync(
            v -> checkApplicantAuthorization(request, applicantId), httpExecutionContext.current())
        .thenComposeAsync(
            v -> {
              DynamicForm form = formFactory.form().bindFromRequest(request);
              ImmutableMap<String, String> formData = cleanForm(form.rawData());
              return applicantService.resetAddressCorrectionWhenAddressChanged(
                  applicantId, programId, blockId, formData);
            },
            httpExecutionContext.current())
        .thenComposeAsync(
            formData ->
                applicantService.stageAndUpdateIfValid(
                    applicantId,
                    programId,
                    blockId,
                    formData,
                    settingsManifest.getEsriAddressServiceAreaValidationEnabled(request)),
            httpExecutionContext.current())
        .thenComposeAsync(
            roApplicantProgramService -> {
              CiviFormProfile profile = profileUtils.currentUserProfileOrThrow(request);
              return renderErrorOrRedirectToNextBlock(
                  request,
                  profile,
                  applicantId,
                  programId,
                  blockId,
                  applicantStage.toCompletableFuture().join(),
                  inReview,
                  ApplicantRequestedAction.getFromString(applicantRequestedAction),
                  roApplicantProgramService);
            },
            httpExecutionContext.current())
        .exceptionally(this::handleUpdateExceptions);
  }

  /** See {@link #updateWithApplicantId}. */
  @Secure
  public CompletionStage<Result> update(
      Request request,
      long programId,
      String blockId,
      boolean inReview,
      String applicantRequestedAction) {
    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return CompletableFuture.completedFuture(redirectToHome());
    }
    return updateWithApplicantId(
        request, applicantId.orElseThrow(), programId, blockId, inReview, applicantRequestedAction);
  }

  private CompletionStage<Result> renderErrorOrRedirectToNextBlock(
      Request request,
      CiviFormProfile profile,
      long applicantId,
      long programId,
      String blockId,
      ApplicantPersonalInfo personalInfo,
      boolean inReview,
      ApplicantRequestedAction applicantRequestedAction,
      ReadOnlyApplicantProgramService roApplicantProgramService) {
    Optional<Block> thisBlockUpdatedMaybe = roApplicantProgramService.getActiveBlock(blockId);
    if (thisBlockUpdatedMaybe.isEmpty()) {
      return failedFuture(new ProgramBlockNotFoundException(programId, blockId));
    }
    Block thisBlockUpdated = thisBlockUpdatedMaybe.get();

    CiviFormProfile submittingProfile = profileUtils.currentUserProfile(request).orElseThrow();

    // Validation errors: re-render this block with errors and previously entered data.
    if (thisBlockUpdated.hasErrors()) {
      ApplicantQuestionRendererParams.ErrorDisplayMode errorDisplayMode;
      if (applicantRequestedAction == ApplicantRequestedAction.REVIEW_PAGE) {
        errorDisplayMode = DISPLAY_ERRORS_WITH_MODAL_REVIEW;
      } else {
        errorDisplayMode = DISPLAY_ERRORS;
      }
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
                          personalInfo,
                          errorDisplayMode,
                          applicantRoutes,
                          submittingProfile))));
    }

    if (settingsManifest.getEsriAddressCorrectionEnabled(request)
        && thisBlockUpdated.hasAddressWithCorrectionEnabled()) {

      AddressQuestion addressQuestion =
          applicantService
              .getFirstAddressCorrectionEnabledApplicantQuestion(thisBlockUpdated)
              .createAddressQuestion();

      if (addressQuestion.needsAddressCorrection()) {

        return applicantService
            .getAddressSuggestionGroup(thisBlockUpdated)
            .thenComposeAsync(
                addressSuggestionGroup ->
                    maybeRenderAddressCorrectionScreen(
                        addressSuggestionGroup,
                        addressQuestion,
                        request,
                        applicantId,
                        programId,
                        blockId,
                        inReview,
                        roApplicantProgramService,
                        thisBlockUpdated,
                        personalInfo));
      }
    }

    try {
      ProgramDefinition programDefinition = programService.getProgramDefinition(programId);
      if (shouldRenderIneligibleBlockView(roApplicantProgramService, programDefinition, blockId)) {
        return supplyAsync(
            () ->
                ok(
                    ineligibleBlockView.render(
                        request,
                        submittingProfile,
                        roApplicantProgramService,
                        messagesApi.preferred(request),
                        applicantId,
                        programDefinition)));
      }
    } catch (ProgramNotFoundException e) {
      notFound(e.toString());
    }

    Map<String, String> flashingMap = new HashMap<>();
    if (roApplicantProgramService.blockHasEligibilityPredicate(blockId)
        && roApplicantProgramService.isActiveBlockEligible(blockId)) {
      flashingMap.put(
          "success-banner",
          messagesApi
              .preferred(request)
              .at(
                  submittingProfile.isTrustedIntermediary()
                      ? MessageKey.TOAST_MAY_QUALIFY_TI.getKeyName()
                      : MessageKey.TOAST_MAY_QUALIFY.getKeyName(),
                  roApplicantProgramService.getProgramTitle()));
    }

    if (applicantRequestedAction == ApplicantRequestedAction.REVIEW_PAGE) {
      return supplyAsync(() -> redirect(applicantRoutes.review(profile, applicantId, programId)));
    }

    Optional<String> nextBlockIdMaybe =
        inReview
            ? roApplicantProgramService.getFirstIncompleteBlockExcludingStatic().map(Block::getId)
            : roApplicantProgramService.getInProgressBlockAfter(blockId).map(Block::getId);
    // No next block so go to the program review page.
    if (nextBlockIdMaybe.isEmpty()) {
      return supplyAsync(() -> redirect(applicantRoutes.review(profile, applicantId, programId)));
    }

    if (inReview) {
      return supplyAsync(
          () ->
              redirect(
                      applicantRoutes.blockReview(
                          profile,
                          applicantId,
                          programId,
                          nextBlockIdMaybe.get(),
                          /* questionName= */ Optional.empty()))
                  .flashing(flashingMap));
    }

    return supplyAsync(
        () ->
            redirect(
                    applicantRoutes.blockEdit(
                        profile,
                        applicantId,
                        programId,
                        nextBlockIdMaybe.get(),
                        /* questionName= */ Optional.empty()))
                .flashing(flashingMap));
  }

  /**
   * Determines if the address entered by the user matches one of the suggestions returned by the
   * {@code EsriClient} and renders the correct screen.
   *
   * <p>If a matching suggestion is found, it is saved to the db and the user is sent on to the next
   * question. Otherwise, the user is directed to a screen where they can pick from the corrected
   * address suggestions returned by the {@code EsriClient}.
   */
  private CompletionStage<Result> maybeRenderAddressCorrectionScreen(
      AddressSuggestionGroup addressSuggestionGroup,
      AddressQuestion addressQuestion,
      Request request,
      long applicantId,
      long programId,
      String blockId,
      boolean inReview,
      ReadOnlyApplicantProgramService roApplicantProgramService,
      Block thisBlockUpdated,
      ApplicantPersonalInfo personalInfo) {
    ImmutableList<AddressSuggestion> suggestions = addressSuggestionGroup.getAddressSuggestions();

    AddressSuggestion[] suggestionMatch =
        suggestions.stream()
            .filter(suggestion -> suggestion.getAddress().equals(addressQuestion.getAddress()))
            .toArray(AddressSuggestion[]::new);

    if (suggestionMatch.length > 0) {
      return confirmAddressWithSuggestions(
          request,
          applicantId,
          programId,
          blockId,
          inReview,
          Optional.of(suggestionMatch[0].getSingleLineAddress()),
          suggestions);
    } else {
      String json = addressSuggestionJsonSerializer.serialize(suggestions);

      Boolean isEligibilityEnabledOnThisBlock =
          thisBlockUpdated.getLeafAddressNodeServiceAreaIds().isPresent();

      CiviFormProfile profile = profileUtils.currentUserProfileOrThrow(request);

      return CompletableFuture.completedFuture(
          ok(addressCorrectionBlockView.render(
                  buildApplicationBaseViewParams(
                      request,
                      applicantId,
                      programId,
                      blockId,
                      inReview,
                      roApplicantProgramService,
                      thisBlockUpdated,
                      personalInfo,
                      ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS,
                      applicantRoutes,
                      profile),
                  messagesApi.preferred(request),
                  addressSuggestionGroup,
                  isEligibilityEnabledOnThisBlock))
              .addingToSession(request, ADDRESS_JSON_SESSION_KEY, json));
    }
  }

  /** Returns true if eligibility is gating and the block is ineligible, false otherwise. */
  private boolean shouldRenderIneligibleBlockView(
      ReadOnlyApplicantProgramService roApplicantProgramService,
      ProgramDefinition programDefinition,
      String blockId) {
    if (programDefinition.eligibilityIsGating()) {
      return !roApplicantProgramService.isActiveBlockEligible(blockId);
    }
    return false;
  }

  private ImmutableMap<String, String> cleanForm(Map<String, String> formData) {
    return formData.entrySet().stream()
        .filter(entry -> !STRIPPED_FORM_FIELDS.contains(entry.getKey()))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private ApplicationBaseView.Params.Builder applicationBaseViewParamsBuilder(
      Request request,
      long applicantId,
      long programId,
      String blockId,
      boolean inReview,
      ReadOnlyApplicantProgramService roApplicantProgramService,
      Block block,
      ApplicantPersonalInfo personalInfo,
      ApplicantQuestionRendererParams.ErrorDisplayMode errorDisplayMode,
      Optional<String> questionName,
      ApplicantRoutes applicantRoutes,
      CiviFormProfile profile) {
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
        .setApplicantPersonalInfo(personalInfo)
        .setPreferredLanguageSupported(roApplicantProgramService.preferredLanguageSupported())
        .setApplicantStorageClient(applicantStorageClient)
        .setBaseUrl(baseUrl)
        .setErrorDisplayMode(errorDisplayMode)
        .setApplicantSelectedQuestionName(questionName)
        .setApplicantRoutes(applicantRoutes)
        .setProfile(profile);
  }

  private ApplicationBaseView.Params buildApplicationBaseViewParams(
      Request request,
      long applicantId,
      long programId,
      String blockId,
      boolean inReview,
      ReadOnlyApplicantProgramService roApplicantProgramService,
      Block block,
      ApplicantPersonalInfo personalInfo,
      ApplicantQuestionRendererParams.ErrorDisplayMode errorDisplayMode,
      ApplicantRoutes applicantRoutes,
      CiviFormProfile profile) {
    return applicationBaseViewParamsBuilder(
            request,
            applicantId,
            programId,
            blockId,
            inReview,
            roApplicantProgramService,
            block,
            personalInfo,
            errorDisplayMode,
            /* questionName= */ Optional.empty(),
            applicantRoutes,
            profile)
        .build();
  }

  private CompletionStage<StoredFileModel> ensureFileRecord(
      String key, Optional<String> originalFileName) {
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
