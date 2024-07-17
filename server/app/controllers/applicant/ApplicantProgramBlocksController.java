package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static controllers.applicant.ApplicantRequestedAction.PREVIOUS_BLOCK;
import static controllers.applicant.ApplicantRequestedAction.REVIEW_PAGE;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS;
import static views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS_WITH_MODAL_PREVIOUS;
import static views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS_WITH_MODAL_REVIEW;

import actions.RouteExtractor;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import controllers.CiviFormController;
import controllers.FlashKey;
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
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Call;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.StoredFileRepository;
import repository.VersionRepository;
import services.AlertSettings;
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
import views.ApplicationBaseViewParams;
import views.applicant.AddressCorrectionBlockView;
import views.applicant.ApplicantFileUploadRenderer;
import views.applicant.ApplicantProgramBlockEditView;
import views.applicant.ApplicantProgramBlockEditViewFactory;
import views.applicant.IneligibleBlockView;
import views.applicant.NorthStarAddressCorrectionBlockView;
import views.applicant.NorthStarApplicantIneligibleView;
import views.applicant.NorthStarApplicantProgramBlockEditView;
import views.components.ToastMessage;
import views.questiontypes.ApplicantQuestionRendererFactory;
import views.questiontypes.ApplicantQuestionRendererParams;

/**
 * Controller for handling an applicant filling out a single program. CAUTION: you must explicitly
 * check the current profile so that an unauthorized user cannot access another applicant's data!
 */
public final class ApplicantProgramBlocksController extends CiviFormController {
  private static final ImmutableSet<String> STRIPPED_FORM_FIELDS = ImmutableSet.of("csrfToken");
  @VisibleForTesting static final String ADDRESS_JSON_SESSION_KEY = "addressJson";

  private final ApplicantService applicantService;
  private final MessagesApi messagesApi;
  private final ClassLoaderExecutionContext classLoaderExecutionContext;
  private final ApplicantProgramBlockEditView editView;
  private final NorthStarApplicantProgramBlockEditView northStarApplicantProgramBlockEditView;
  private final FormFactory formFactory;
  private final ApplicantStorageClient applicantStorageClient;
  private final StoredFileRepository storedFileRepository;
  private final SettingsManifest settingsManifest;
  private final String baseUrl;
  private final IneligibleBlockView ineligibleBlockView;
  private final NorthStarApplicantIneligibleView northStarApplicantIneligibleView;
  private final AddressCorrectionBlockView addressCorrectionBlockView;
  private final NorthStarAddressCorrectionBlockView northStarAddressCorrectionBlockView;
  private final AddressSuggestionJsonSerializer addressSuggestionJsonSerializer;
  private final ProgramService programService;
  private final ApplicantRoutes applicantRoutes;
  private final EligibilityAlertSettingsCalculator eligibilityAlertSettingsCalculator;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject
  public ApplicantProgramBlocksController(
      ApplicantService applicantService,
      MessagesApi messagesApi,
      ClassLoaderExecutionContext classLoaderExecutionContext,
      ApplicantProgramBlockEditViewFactory editViewFactory,
      NorthStarApplicantProgramBlockEditView northStarApplicantProgramBlockEditView,
      FormFactory formFactory,
      ApplicantStorageClient applicantStorageClient,
      StoredFileRepository storedFileRepository,
      ProfileUtils profileUtils,
      Config configuration,
      SettingsManifest settingsManifest,
      ApplicantFileUploadRenderer applicantFileUploadRenderer,
      IneligibleBlockView ineligibleBlockView,
      NorthStarApplicantIneligibleView northStarApplicantIneligibleView,
      AddressCorrectionBlockView addressCorrectionBlockView,
      NorthStarAddressCorrectionBlockView northStarAddressCorrectionBlockView,
      AddressSuggestionJsonSerializer addressSuggestionJsonSerializer,
      ProgramService programService,
      VersionRepository versionRepository,
      ApplicantRoutes applicantRoutes,
      EligibilityAlertSettingsCalculator eligibilityAlertSettingsCalculator) {
    super(profileUtils, versionRepository);
    this.applicantService = checkNotNull(applicantService);
    this.messagesApi = checkNotNull(messagesApi);
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.formFactory = checkNotNull(formFactory);
    this.applicantStorageClient = checkNotNull(applicantStorageClient);
    this.storedFileRepository = checkNotNull(storedFileRepository);
    this.baseUrl = checkNotNull(configuration).getString("base_url");
    this.settingsManifest = checkNotNull(settingsManifest);
    this.ineligibleBlockView = checkNotNull(ineligibleBlockView);
    this.northStarApplicantIneligibleView = checkNotNull(northStarApplicantIneligibleView);
    this.addressCorrectionBlockView = checkNotNull(addressCorrectionBlockView);
    this.addressSuggestionJsonSerializer = checkNotNull(addressSuggestionJsonSerializer);
    this.applicantRoutes = checkNotNull(applicantRoutes);
    this.eligibilityAlertSettingsCalculator = checkNotNull(eligibilityAlertSettingsCalculator);
    this.editView =
        editViewFactory.create(new ApplicantQuestionRendererFactory(applicantFileUploadRenderer));
    this.northStarApplicantProgramBlockEditView =
        checkNotNull(northStarApplicantProgramBlockEditView);
    this.northStarAddressCorrectionBlockView = checkNotNull(northStarAddressCorrectionBlockView);
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
      Request request,
      long applicantId,
      long programId,
      String blockId,
      boolean inReview,
      ApplicantRequestedActionWrapper applicantRequestedActionWrapper) {
    DynamicForm form = formFactory.form().bindFromRequest(request);
    Optional<String> selectedAddress =
        Optional.ofNullable(form.get(AddressCorrectionBlockView.SELECTED_ADDRESS_NAME));
    Optional<String> maybeAddressJson = request.session().get(ADDRESS_JSON_SESSION_KEY);

    ImmutableList<AddressSuggestion> suggestions =
        addressSuggestionJsonSerializer.deserialize(
            maybeAddressJson.orElseThrow(() -> new RuntimeException("Address JSON missing")));
    return confirmAddressWithSuggestions(
        request,
        applicantId,
        programId,
        blockId,
        inReview,
        selectedAddress,
        suggestions,
        applicantRequestedActionWrapper.getAction());
  }

  /** Handles the applicant's selection from the address correction options. */
  @Secure
  public CompletionStage<Result> confirmAddress(
      Request request,
      long programId,
      String blockId,
      boolean inReview,
      ApplicantRequestedActionWrapper applicantRequestedActionWrapper) {
    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return CompletableFuture.completedFuture(redirectToHome());
    }
    return confirmAddressWithApplicantId(
        request,
        applicantId.orElseThrow(),
        programId,
        blockId,
        inReview,
        applicantRequestedActionWrapper);
  }

  /** Saves the selected corrected address to the db and redirects the user to the next screen */
  private CompletionStage<Result> confirmAddressWithSuggestions(
      Request request,
      long applicantId,
      long programId,
      String blockId,
      boolean inReview,
      Optional<String> selectedAddress,
      ImmutableList<AddressSuggestion> suggestions,
      ApplicantRequestedAction applicantRequestedAction) {
    CompletableFuture<ApplicantPersonalInfo> applicantStage =
        applicantService.getPersonalInfo(applicantId).toCompletableFuture();

    return CompletableFuture.allOf(
            checkApplicantAuthorization(request, applicantId), applicantStage)
        .thenComposeAsync(v -> checkProgramAuthorization(request, programId))
        .thenComposeAsync(
            v ->
                applicantService.getCorrectedAddress(
                    applicantId, programId, blockId, selectedAddress, suggestions),
            classLoaderExecutionContext.current())
        .thenComposeAsync(
            questionPathToValueMap ->
                applicantService.stageAndUpdateIfValid(
                    applicantId,
                    programId,
                    blockId,
                    cleanForm(questionPathToValueMap),
                    settingsManifest.getEsriAddressServiceAreaValidationEnabled(request)),
            classLoaderExecutionContext.current())
        .thenComposeAsync(
            roApplicantProgramService -> {
              removeAddressJsonFromSession(request);
              CiviFormProfile profile = profileUtils.currentUserProfileOrThrow(request);
              return renderErrorOrRedirectToRequestedPage(
                  request,
                  profile,
                  applicantId,
                  programId,
                  blockId,
                  applicantStage.join(),
                  inReview,
                  applicantRequestedAction,
                  roApplicantProgramService);
            },
            classLoaderExecutionContext.current())
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
                classLoaderExecutionContext.current())
            .toCompletableFuture();

    CiviFormProfile profile = profileUtils.currentUserProfileOrThrow(request);

    CompletableFuture<ReadOnlyApplicantProgramService> applicantProgramServiceCompletableFuture =
        applicantStage
            .thenComposeAsync(v -> checkProgramAuthorization(request, programId))
            .thenComposeAsync(
                v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
                classLoaderExecutionContext.current())
            .toCompletableFuture();

    return CompletableFuture.allOf(
            applicantAuthCompletableFuture, applicantProgramServiceCompletableFuture)
        .thenApplyAsync(
            (v) -> {
              Optional<Result> applicationUpdatedOptional =
                  updateApplicationToLatestProgramVersionIfNeeded(applicantId, programId, request);
              if (applicationUpdatedOptional.isPresent()) {
                return applicationUpdatedOptional.get();
              }

              ReadOnlyApplicantProgramService roApplicantProgramService =
                  applicantProgramServiceCompletableFuture.join();
              ImmutableList<Block> blocks = roApplicantProgramService.getAllActiveBlocks();
              String blockId = blocks.get(previousBlockIndex).getId();
              Optional<Block> block = roApplicantProgramService.getActiveBlock(blockId);

              if (block.isPresent()) {
                ApplicantPersonalInfo personalInfo = applicantStage.toCompletableFuture().join();
                ApplicationBaseViewParams applicationParams =
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
                        profile);
                if (settingsManifest.getNorthStarApplicantUi(request)) {
                  return ok(northStarApplicantProgramBlockEditView.render(
                          request, applicationParams))
                      .as(Http.MimeTypes.HTML);
                } else {
                  return ok(editView.render(applicationParams));
                }
              } else {
                return notFound();
              }
            },
            classLoaderExecutionContext.current())
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

    Optional<String> successBannerMessage = request.flash().get(FlashKey.SUCCESS_BANNER);
    Optional<ToastMessage> flashSuccessBanner =
        successBannerMessage.map(m -> ToastMessage.success(m));

    return applicantStage
        .thenComposeAsync(
            v -> checkApplicantAuthorization(request, applicantId),
            classLoaderExecutionContext.current())
        .thenComposeAsync(v -> checkProgramAuthorization(request, programId))
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            classLoaderExecutionContext.current())
        .thenApplyAsync(
            (roApplicantProgramService) -> {
              Optional<Result> applicationUpdatedOptional =
                  updateApplicationToLatestProgramVersionIfNeeded(applicantId, programId, request);
              if (applicationUpdatedOptional.isPresent()) {
                return applicationUpdatedOptional.get();
              }

              Optional<Block> block = roApplicantProgramService.getActiveBlock(blockId);

              if (block.isPresent()) {
                ApplicantPersonalInfo personalInfo = applicantStage.toCompletableFuture().join();
                CiviFormProfile profile = profileUtils.currentUserProfileOrThrow(request);
                ApplicationBaseViewParams applicationParams =
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
                        .setBannerToastMessage(flashSuccessBanner)
                        .setBannerMessage(successBannerMessage)
                        .build();
                if (settingsManifest.getNorthStarApplicantUi(request)) {
                  return ok(northStarApplicantProgramBlockEditView.render(
                          request, applicationParams))
                      .as(Http.MimeTypes.HTML);
                } else {
                  return ok(editView.render(applicationParams));
                }
              } else {
                return notFound();
              }
            },
            classLoaderExecutionContext.current())
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
   * file key in the query string. We add this to the list of uploaded files, or if it's already
   * present, do nothing.
   *
   * <p>After adding the file, the current question block is reloaded, showing the user the uploaded
   * file.
   */
  @Secure
  public CompletionStage<Result> addFile(
      Request request, long programId, String blockId, boolean inReview) {
    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return CompletableFuture.completedFuture(redirectToHome());
    }
    return addFileWithApplicantId(request, applicantId.orElseThrow(), programId, blockId, inReview);
  }

  @Secure
  public CompletionStage<Result> addFileWithApplicantId(
      Request request, long applicantId, long programId, String blockId, boolean inReview) {
    CompletionStage<ApplicantPersonalInfo> applicantStage =
        this.applicantService.getPersonalInfo(applicantId);

    return applicantStage
        .thenComposeAsync(
            v -> checkApplicantAuthorization(request, applicantId),
            classLoaderExecutionContext.current())
        .thenComposeAsync(
            v -> checkProgramAuthorization(request, programId),
            classLoaderExecutionContext.current())
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            classLoaderExecutionContext.current())
        .thenComposeAsync(
            (roApplicantProgramService) -> {
              Optional<Block> block = roApplicantProgramService.getActiveBlock(blockId);

              if (block.isEmpty() || !block.get().isFileUpload()) {
                return failedFuture(new ProgramBlockNotFoundException(programId, blockId));
              }

              Optional<String> bucket = request.queryString("bucket");
              Optional<String> key = request.queryString("key");

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
              Optional<ImmutableList<String>> keysOptional =
                  fileUploadQuestion.getFileKeyListValue();

              if (keysOptional.isPresent()) {
                ImmutableList<String> keys = keysOptional.get();

                boolean appendValue = true;

                // Write the existing keys so that we don't delete any.
                for (int i = 0; i < keys.size(); i++) {
                  String keyValue = keys.get(i);
                  fileUploadQuestionFormData.put(
                      fileUploadQuestion.getFileKeyListPathForIndex(i).toString(), keyValue);
                  // Key already exists in question, no need to append it. But we may want to render
                  // some kind of error in this case in the future, since it means the user
                  // essentially "replaced" whatever
                  // file already existed with that same name. Alternatively, we could prevent this
                  // on the client-side.
                  if (keyValue.equals(key.get())) {
                    appendValue = false;
                  }
                }

                if (appendValue) {
                  fileUploadQuestionFormData.put(
                      fileUploadQuestion.getFileKeyListPathForIndex(keys.size()).toString(),
                      key.get());
                }
              } else {
                fileUploadQuestionFormData.put(
                    fileUploadQuestion.getFileKeyListPathForIndex(0).toString(), key.get());
              }

              return ensureFileRecord(key.get(), Optional.empty())
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
            classLoaderExecutionContext.current())
        .thenComposeAsync(
            roApplicantProgramService -> {
              Optional<Block> block = roApplicantProgramService.getActiveBlock(blockId);

              if (block.isEmpty() || !block.get().isFileUpload()) {
                return failedFuture(new ProgramBlockNotFoundException(programId, blockId));
              }

              // Re-render the current page, with the updated file info.
              return supplyAsync(
                  () -> {
                    ApplicantPersonalInfo personalInfo =
                        applicantStage.toCompletableFuture().join();
                    CiviFormProfile submittingProfile =
                        profileUtils.currentUserProfileOrThrow(request);

                    ApplicationBaseViewParams applicationParams =
                        buildApplicationBaseViewParams(
                            request,
                            applicantId,
                            programId,
                            blockId,
                            inReview,
                            roApplicantProgramService,
                            block.get(),
                            personalInfo,
                            DISPLAY_ERRORS,
                            applicantRoutes,
                            submittingProfile);
                    if (settingsManifest.getNorthStarApplicantUi(request)) {
                      return ok(northStarApplicantProgramBlockEditView.render(
                              request, applicationParams))
                          .as(Http.MimeTypes.HTML);
                    } else {
                      return ok(editView.render(applicationParams));
                    }
                  });
            },
            classLoaderExecutionContext.current())
        .exceptionally(this::handleUpdateExceptions);
  }

  /**
   * Used by the file upload question. We let users directly upload files to S3 bucket from
   * browsers. On success, users are redirected to this method. The redirect is a GET method with
   * file key in the query string. We parse and store them in the database for record and redirect
   * users to the next block or review page.
   */
  @Secure
  public CompletionStage<Result> updateFileWithApplicantId(
      Request request,
      long applicantId,
      long programId,
      String blockId,
      boolean inReview,
      ApplicantRequestedActionWrapper applicantRequestedActionWrapper) {
    CompletionStage<ApplicantPersonalInfo> applicantStage =
        this.applicantService.getPersonalInfo(applicantId);

    return applicantStage
        .thenComposeAsync(
            v -> checkApplicantAuthorization(request, applicantId),
            classLoaderExecutionContext.current())
        .thenComposeAsync(v -> checkProgramAuthorization(request, programId))
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            classLoaderExecutionContext.current())
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
            classLoaderExecutionContext.current())
        .thenComposeAsync(
            (roApplicantProgramService) -> {
              CiviFormProfile profile = profileUtils.currentUserProfileOrThrow(request);
              return renderErrorOrRedirectToRequestedPage(
                  request,
                  profile,
                  applicantId,
                  programId,
                  blockId,
                  applicantStage.toCompletableFuture().join(),
                  inReview,
                  applicantRequestedActionWrapper.getAction(),
                  roApplicantProgramService);
            },
            classLoaderExecutionContext.current())
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
      Request request,
      long programId,
      String blockId,
      boolean inReview,
      ApplicantRequestedActionWrapper applicantRequestedActionWrapper) {
    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return CompletableFuture.completedFuture(redirectToHome());
    }
    return updateFileWithApplicantId(
        request,
        applicantId.orElseThrow(),
        programId,
        blockId,
        inReview,
        applicantRequestedActionWrapper);
  }

  /**
   * Accepts, validates and saves submission of applicant data for {@code blockId}.
   *
   * <p>Returns the applicable next step in the flow:
   *
   * <ul>
   *   <li>If there are errors, then renders the edit page for the same block with the errors shown.
   *   <li>If {@code applicantRequestedActionWrapper#getAction} is the {@link
   *       ApplicantRequestedAction#REVIEW_PAGE}, then renders the review page.
   *   <li>If {@code applicantRequestedActionWrapper#getAction} is the {@link
   *       ApplicantRequestedAction#PREVIOUS_BLOCK}, then renders the previous block (or the review
   *       page if the applicant is currently on the first block).
   *   <li>If {@code applicantRequestedActionWrapper#getAction} is the {@link
   *       ApplicantRequestedAction#NEXT_BLOCK}, then we use {@code inReview} to determine what
   *       block to show next:
   *       <ul>
   *         <li>If {@code inReview}, then renders the next incomplete block.
   *         <li>If not {@code inReview}, then renders the next visible block.
   *         <li>If there is no next block, then renders the review page.
   *       </ul>
   * </ul>
   */
  @Secure
  public CompletionStage<Result> updateWithApplicantId(
      Request request,
      long applicantId,
      long programId,
      String blockId,
      boolean inReview,
      ApplicantRequestedActionWrapper applicantRequestedActionWrapper) {
    CompletionStage<ApplicantPersonalInfo> applicantStage =
        this.applicantService.getPersonalInfo(applicantId);

    CompletableFuture<ImmutableMap<String, String>> formDataCompletableFuture =
        applicantStage
            .thenComposeAsync(
                v -> checkApplicantAuthorization(request, applicantId),
                classLoaderExecutionContext.current())
            .thenComposeAsync(
                v -> {
                  DynamicForm form = formFactory.form().bindFromRequest(request);
                  ImmutableMap<String, String> formData = cleanForm(form.rawData());
                  return applicantService.resetAddressCorrectionWhenAddressChanged(
                      applicantId, programId, blockId, formData);
                },
                classLoaderExecutionContext.current())
            .thenComposeAsync(
                formData ->
                    applicantService.setPhoneCountryCode(applicantId, programId, blockId, formData),
                classLoaderExecutionContext.current())
            .thenComposeAsync(
                formData ->
                    applicantService.cleanDateQuestions(applicantId, programId, blockId, formData),
                classLoaderExecutionContext.current())
            .toCompletableFuture();
    CompletableFuture<ReadOnlyApplicantProgramService> applicantProgramServiceCompletableFuture =
        applicantStage
            .thenComposeAsync(v -> checkProgramAuthorization(request, programId))
            .thenComposeAsync(
                v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
                classLoaderExecutionContext.current())
            .toCompletableFuture();

    return CompletableFuture.allOf(
            formDataCompletableFuture, applicantProgramServiceCompletableFuture)
        .thenComposeAsync(
            (v) -> {
              Optional<Result> applicationUpdatedOptional =
                  updateApplicationToLatestProgramVersionIfNeeded(applicantId, programId, request);
              if (applicationUpdatedOptional.isPresent()) {
                return CompletableFuture.completedFuture(applicationUpdatedOptional.get());
              }

              ImmutableMap<String, String> formData = formDataCompletableFuture.join();
              ReadOnlyApplicantProgramService readOnlyApplicantProgramService =
                  applicantProgramServiceCompletableFuture.join();
              CiviFormProfile profile = profileUtils.currentUserProfileOrThrow(request);
              Optional<Block> optionalBlockBeforeUpdate =
                  readOnlyApplicantProgramService.getActiveBlock(blockId);
              ApplicantRequestedAction applicantRequestedAction =
                  applicantRequestedActionWrapper.getAction();

              if (canNavigateAwayFromBlockImmediately(
                  formData, applicantRequestedAction, optionalBlockBeforeUpdate)) {
                return redirectToRequestedPage(
                    profile,
                    applicantId,
                    programId,
                    blockId,
                    inReview,
                    applicantRequestedAction,
                    readOnlyApplicantProgramService,
                    /* flashingMap= */ ImmutableMap.of());
              }
              return applicantService
                  .stageAndUpdateIfValid(
                      applicantId,
                      programId,
                      blockId,
                      formData,
                      settingsManifest.getEsriAddressServiceAreaValidationEnabled(request))
                  .thenComposeAsync(
                      newReadOnlyApplicantProgramService ->
                          renderErrorOrRedirectToRequestedPage(
                              request,
                              profile,
                              applicantId,
                              programId,
                              blockId,
                              applicantStage.toCompletableFuture().join(),
                              inReview,
                              applicantRequestedAction,
                              newReadOnlyApplicantProgramService),
                      classLoaderExecutionContext.current());
            })
        .exceptionally(this::handleUpdateExceptions);
  }

  /**
   * Returns true if applicants can immediately navigate away from a block because they haven't even
   * started answering it. Returns false if applicants have started answering the block or answered
   * the block in the past.
   */
  private boolean canNavigateAwayFromBlockImmediately(
      ImmutableMap<String, String> formData,
      ApplicantRequestedAction applicantRequestedAction,
      Optional<Block> optionalBlockBeforeUpdate) {
    // An applicant can immediately navigate away from a block if:
    // There are no answers currently filled out...
    return formData.values().stream().allMatch(value -> value.equals(""))
        // ... and the applicant wants to navigate to previous or review...
        // [Explanation: We can't let applicants navigate to the next block without
        // answers because the next block may have a visibility condition that
        // depends on the answers to this block.]
        && (applicantRequestedAction == REVIEW_PAGE || applicantRequestedAction == PREVIOUS_BLOCK)
        // ... and the applicant didn't previously complete this block...
        && optionalBlockBeforeUpdate.isPresent()
        && !optionalBlockBeforeUpdate.get().isCompletedInProgramWithoutErrors()
        // ...and there's at least one required question, meaning that all blank answers
        // is *not* a valid response.
        // [Explanation: We don't allow applicants to submit an application until they've
        // at least seen all the questions. We mark as question as "seen" by checking
        // that the metadata is filled in (see
        // {@link ApplicantQuestion#isAnsweredOrSkippedOptionalInProgram}).
        //
        // If a block has all optional questions, having all empty answers is a valid
        // response and we should mark that block as seen by having
        // {@link #stageAndUpdateIfValid} fill in that metadata. So, we can't immediately
        // navigate away.]
        && !optionalBlockBeforeUpdate.get().hasOnlyOptionalQuestions();
  }

  /** See {@link #updateWithApplicantId}. */
  @Secure
  public CompletionStage<Result> update(
      Request request,
      long programId,
      String blockId,
      boolean inReview,
      ApplicantRequestedActionWrapper applicantRequestedActionWrapper) {
    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return CompletableFuture.completedFuture(redirectToHome());
    }
    return updateWithApplicantId(
        request,
        applicantId.orElseThrow(),
        programId,
        blockId,
        inReview,
        applicantRequestedActionWrapper);
  }

  /**
   * Checks if the block specified by {@code blockId} is valid or has errors. If it has errors, then
   * the same block is re-rendered with the errors displayed. Otherwise, the applicant is redirected
   * to the page specified by {@code applicantRequestedAction}.
   *
   * @param applicantRequestedAction the page the applicant would like to see next if there are no
   *     errors with this block.
   */
  private CompletionStage<Result> renderErrorOrRedirectToRequestedPage(
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
      if (applicantRequestedAction == REVIEW_PAGE) {
        errorDisplayMode = DISPLAY_ERRORS_WITH_MODAL_REVIEW;
      } else if (applicantRequestedAction == PREVIOUS_BLOCK) {
        errorDisplayMode = DISPLAY_ERRORS_WITH_MODAL_PREVIOUS;
      } else {
        errorDisplayMode = DISPLAY_ERRORS;
      }
      return supplyAsync(
          () -> {
            ApplicationBaseViewParams applicationParams =
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
                    submittingProfile);
            if (settingsManifest.getNorthStarApplicantUi(request)) {
              return ok(northStarApplicantProgramBlockEditView.render(request, applicationParams))
                  .as(Http.MimeTypes.HTML);
            } else {
              return ok(editView.render(applicationParams));
            }
          });
    }

    // TODO(#7893): When you enter an address that requires correction but then click "Previous",
    // you're still taken forward to the address correction screen which is unexpected.
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
                        personalInfo,
                        applicantRequestedAction));
      }
    }

    try {
      ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
      if (shouldRenderIneligibleBlockView(roApplicantProgramService, programDefinition, blockId)) {
        return renderIneligiblePage(
            request,
            submittingProfile,
            applicantId,
            personalInfo,
            roApplicantProgramService,
            programDefinition);
      }
    } catch (ProgramNotFoundException e) {
      notFound(e.toString());
    }

    Map<String, String> flashingMap = new HashMap<>();

    return redirectToRequestedPage(
        profile,
        applicantId,
        programId,
        blockId,
        inReview,
        applicantRequestedAction,
        roApplicantProgramService,
        flashingMap);
  }

  private CompletionStage<Result> renderIneligiblePage(
      Request request,
      CiviFormProfile profile,
      long applicantId,
      ApplicantPersonalInfo personalInfo,
      ReadOnlyApplicantProgramService roApplicantProgramService,
      ProgramDefinition programDefinition) {
    if (settingsManifest.getNorthStarApplicantUi(request)) {
      NorthStarApplicantIneligibleView.Params params =
          NorthStarApplicantIneligibleView.Params.builder()
              .setRequest(request)
              .setApplicantId(applicantId)
              .setProfile(profile)
              .setApplicantPersonalInfo(personalInfo)
              .setProgramDefinition(programDefinition)
              .setRoApplicantProgramService(roApplicantProgramService)
              .setMessages(messagesApi.preferred(request))
              .build();
      return supplyAsync(
          () -> ok(northStarApplicantIneligibleView.render(params)).as(Http.MimeTypes.HTML));
    } else {
      return supplyAsync(
          () ->
              ok(
                  ineligibleBlockView.render(
                      request,
                      profile,
                      roApplicantProgramService,
                      messagesApi.preferred(request),
                      applicantId,
                      programDefinition)));
    }
  }

  /** Returns the correct page based on the given {@code applicantRequestedAction}. */
  private CompletionStage<Result> redirectToRequestedPage(
      CiviFormProfile profile,
      long applicantId,
      long programId,
      String blockId,
      boolean inReview,
      ApplicantRequestedAction applicantRequestedAction,
      ReadOnlyApplicantProgramService roApplicantProgramService,
      Map<String, String> flashingMap) {
    if (applicantRequestedAction == REVIEW_PAGE) {
      return supplyAsync(() -> redirect(applicantRoutes.review(profile, applicantId, programId)));
    }
    if (applicantRequestedAction == PREVIOUS_BLOCK) {
      int currentBlockIndex = roApplicantProgramService.getBlockIndex(blockId);
      return supplyAsync(
          () ->
              redirect(
                  applicantRoutes
                      .blockPreviousOrReview(
                          profile, applicantId, programId, currentBlockIndex, inReview)
                      .url()));
    }

    Optional<String> nextBlockIdMaybe =
        inReview
            ? roApplicantProgramService.getFirstIncompleteBlockExcludingStatic().map(Block::getId)
            : roApplicantProgramService.getInProgressBlockAfter(blockId).map(Block::getId);
    return nextBlockIdMaybe
        .map(
            nextBlockId ->
                supplyAsync(
                    () ->
                        redirect(
                                applicantRoutes.blockEditOrBlockReview(
                                    profile, applicantId, programId, nextBlockId, inReview))
                            .flashing(flashingMap)))
        // No next block so go to the program review page.
        .orElseGet(
            () ->
                supplyAsync(
                    () -> redirect(applicantRoutes.review(profile, applicantId, programId))));
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
      ApplicantPersonalInfo personalInfo,
      ApplicantRequestedAction applicantRequestedAction) {
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
          suggestions,
          applicantRequestedAction);
    } else {
      String json = addressSuggestionJsonSerializer.serialize(suggestions);

      Boolean isEligibilityEnabledOnThisBlock =
          thisBlockUpdated.getLeafAddressNodeServiceAreaIds().isPresent();

      CiviFormProfile profile = profileUtils.currentUserProfileOrThrow(request);

      ApplicationBaseViewParams applicationParams =
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
              profile);
      if (settingsManifest.getNorthStarApplicantUi(request)) {
        return CompletableFuture.completedFuture(
            ok(northStarAddressCorrectionBlockView.render(
                    request,
                    applicationParams,
                    addressSuggestionGroup,
                    applicantRequestedAction,
                    isEligibilityEnabledOnThisBlock))
                .as(Http.MimeTypes.HTML)
                .addingToSession(request, ADDRESS_JSON_SESSION_KEY, json));
      } else {
        return CompletableFuture.completedFuture(
            ok(addressCorrectionBlockView.render(
                    applicationParams,
                    messagesApi.preferred(request),
                    addressSuggestionGroup,
                    applicantRequestedAction,
                    isEligibilityEnabledOnThisBlock))
                .addingToSession(request, ADDRESS_JSON_SESSION_KEY, json));
      }
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

  private ApplicationBaseViewParams.Builder applicationBaseViewParamsBuilder(
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

    AlertSettings eligibilityAlertSettings =
        eligibilityAlertSettingsCalculator.calculate(
            request,
            profileUtils.currentUserProfile(request).get().isTrustedIntermediary(),
            !roApplicantProgramService.isApplicationNotEligible(),
            programId);

    return ApplicationBaseViewParams.builder()
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
        .setProfile(profile)
        .setBlockList(roApplicantProgramService.getAllActiveBlocks())
        .setEligibilityAlertSettings(eligibilityAlertSettings);
  }

  private ApplicationBaseViewParams buildApplicationBaseViewParams(
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
          || cause instanceof UnsupportedScalarTypeException) {
        logger.error("Exception while updating applicant data", cause);
        return badRequest("Unable to process this request");
      }
      throw new RuntimeException(cause);
    }
    throw new RuntimeException(throwable);
  }

  /**
   * Check if the application needs to be updated to a newer program version. If it does, update and
   * return a redirect result back to the review page
   *
   * @return {@link Result} if application was updated; empty if not
   */
  private Optional<Result> updateApplicationToLatestProgramVersionIfNeeded(
      long applicantId, long programId, Request request) {
    if (settingsManifest.getFastforwardEnabled(request)) {
      Optional<Long> latestProgramId =
          applicantService.updateApplicationToLatestProgramVersion(applicantId, programId);

      RouteExtractor routeExtractor = new RouteExtractor(request);

      if (latestProgramId.isPresent()) {
        Call redirectLocation =
            routeExtractor.containsKey("applicantId")
                ? controllers.applicant.routes.ApplicantProgramReviewController
                    .reviewWithApplicantId(applicantId, latestProgramId.get())
                : controllers.applicant.routes.ApplicantProgramReviewController.review(
                    latestProgramId.get());

        return Optional.of(
            redirect(redirectLocation.url())
                .flashing(FlashKey.SHOW_FAST_FORWARDED_MESSAGE, "true"));
      }
    }

    return Optional.empty();
  }
}
