package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static views.applicant.AuthenticateUpsellCreator.createLoginPromptModal;
import static views.components.Modal.RepeatOpenBehavior.Group.PROGRAM_SLUG_LOGIN_PROMPT;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import auth.controllers.MissingOptionalException;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.ApplicationModel;
import org.pac4j.play.java.Secure;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Call;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import services.MessageKey;
import services.applicant.AnswerData;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantPersonalInfo.ApplicantType;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applicant.exception.ApplicationNotEligibleException;
import services.applicant.exception.ApplicationOutOfDateException;
import services.applicant.exception.ApplicationSubmissionException;
import services.applicant.exception.DuplicateApplicationException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import views.applicant.ApplicantProgramSummaryView;
import views.applicant.IneligibleBlockView;
import views.applicant.NorthStarApplicantProgramSummaryView;
import views.applicant.PreventDuplicateSubmissionView;
import views.components.Modal;
import views.components.Modal.RepeatOpenBehavior;
import views.components.ToastMessage;

/**
 * Controller for reviewing program responses for an applicant.
 *
 * <p>CAUTION: You must explicitly check the current profile so that an unauthorized user cannot
 * access another applicant's data!
 */
public class ApplicantProgramReviewController extends CiviFormController {

  private final ApplicantService applicantService;
  private final HttpExecutionContext classLoaderExecutionContext;
  private final MessagesApi messagesApi;
  private final ApplicantProgramSummaryView summaryView;
  private final NorthStarApplicantProgramSummaryView northStarSummaryView;
  private final IneligibleBlockView ineligibleBlockView;
  private final PreventDuplicateSubmissionView preventDuplicateSubmissionView;
  private final SettingsManifest settingsManifest;
  private final ProgramService programService;
  private final ApplicantRoutes applicantRoutes;

  @Inject
  public ApplicantProgramReviewController(
      ApplicantService applicantService,
      HttpExecutionContext classLoaderExecutionContext,
      MessagesApi messagesApi,
      ApplicantProgramSummaryView summaryView,
      NorthStarApplicantProgramSummaryView northStarSummaryView,
      IneligibleBlockView ineligibleBlockView,
      PreventDuplicateSubmissionView preventDuplicateSubmissionView,
      ProfileUtils profileUtils,
      SettingsManifest settingsManifest,
      ProgramService programService,
      VersionRepository versionRepository,
      ApplicantRoutes applicantRoutes) {
    super(profileUtils, versionRepository);
    this.applicantService = checkNotNull(applicantService);
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.messagesApi = checkNotNull(messagesApi);
    this.summaryView = checkNotNull(summaryView);
    this.northStarSummaryView = checkNotNull(northStarSummaryView);
    this.ineligibleBlockView = checkNotNull(ineligibleBlockView);
    this.preventDuplicateSubmissionView = checkNotNull(preventDuplicateSubmissionView);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.programService = checkNotNull(programService);
    this.applicantRoutes = checkNotNull(applicantRoutes);
  }

  @Secure
  public CompletionStage<Result> reviewWithApplicantId(
      Request request, long applicantId, long programId) {
    Optional<CiviFormProfile> submittingProfile = profileUtils.currentUserProfile(request);

    // If the user isn't already logged in within their browser session, send them home.
    if (submittingProfile.isEmpty()) {
      return CompletableFuture.completedFuture(redirectToHome());
    }

    boolean isTrustedIntermediary = submittingProfile.get().isTrustedIntermediary();
    Optional<ToastMessage> flashBanner =
        request.flash().get("banner").map(m -> ToastMessage.alert(m));
    Optional<ToastMessage> flashSuccessBanner =
        request.flash().get("success-banner").map(m -> ToastMessage.success(m));
    CompletionStage<ApplicantPersonalInfo> applicantStage =
        applicantService.getPersonalInfo(applicantId, request);

    return applicantStage
        .thenComposeAsync(v -> checkApplicantAuthorization(request, applicantId))
        .thenComposeAsync(v -> checkProgramAuthorization(request, programId))
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            classLoaderExecutionContext.current())
        .thenApplyAsync(
            (roApplicantProgramService) -> {
              Messages messages = messagesApi.preferred(request);
              Optional<ToastMessage> notEligibleBanner = Optional.empty();
              try {
                if (shouldShowNotEligibleBanner(roApplicantProgramService, programId)) {
                  notEligibleBanner =
                      Optional.of(
                          ToastMessage.alert(
                              messages.at(
                                  isTrustedIntermediary
                                      ? MessageKey.TOAST_MAY_NOT_QUALIFY_TI.getKeyName()
                                      : MessageKey.TOAST_MAY_NOT_QUALIFY.getKeyName(),
                                  roApplicantProgramService.getProgramTitle())));
                }
              } catch (ProgramNotFoundException e) {
                return notFound(e.toString());
              }
              ApplicantProgramSummaryView.Params.Builder params =
                  this.generateParamsBuilder(roApplicantProgramService)
                      .setApplicantId(applicantId)
                      .setApplicantPersonalInfo(applicantStage.toCompletableFuture().join())
                      .setBannerMessages(
                          ImmutableList.of(flashBanner, flashSuccessBanner, notEligibleBanner))
                      .setMessages(messages)
                      .setProgramId(programId)
                      .setRequest(request)
                      .setProfile(
                          submittingProfile.orElseThrow(
                              () -> new MissingOptionalException(CiviFormProfile.class)));

              // Show a login prompt on the review page if we were redirected from a program slug
              // and user is a guest.
              if (request.flash().get("redirected-from-program-slug").isPresent()
                  && applicantStage.toCompletableFuture().join().getType() == ApplicantType.GUEST) {
                Modal loginPromptModal =
                    createLoginPromptModal(
                            messages,
                            /* postLoginRedirectTo= */ routes.ApplicantProgramsController.show(
                                    request.flash().get("redirected-from-program-slug").get())
                                .url(),
                            messages.at(
                                MessageKey.INITIAL_LOGIN_MODAL_PROMPT.getKeyName(),
                                // The applicant portal name should always be set (there is a
                                // default setting as well).
                                settingsManifest.getApplicantPortalName(request).get()),
                            MessageKey.BUTTON_CONTINUE_TO_APPLICATION)
                        .setDisplayOnLoad(true)
                        .setRepeatOpenBehavior(
                            RepeatOpenBehavior.showOnlyOnce(PROGRAM_SLUG_LOGIN_PROMPT))
                        .build();
                params.setLoginPromptModal(loginPromptModal);
              }
              if (settingsManifest.getNorthStarApplicantUi(request)) {
                int totalBlockCount = roApplicantProgramService.getAllActiveBlocks().size();
                int completedBlockCount = roApplicantProgramService.getActiveAndCompletedInProgramBlockCount();

                NorthStarApplicantProgramSummaryView.Params northStarParams =
                    NorthStarApplicantProgramSummaryView.Params.builder()
                        .setBlocks(roApplicantProgramService.getAllActiveBlocks())
                        .setApplicantId(applicantId)
                        .setProfile(
                            submittingProfile.orElseThrow(
                                () -> new MissingOptionalException(CiviFormProfile.class)))
                        .setProgramId(programId)
                        .setCompletedBlockCount(completedBlockCount)
                        .setTotalBlockCount(totalBlockCount)
                        .build();
                return ok(northStarSummaryView.render(request, northStarParams))
                    .as(Http.MimeTypes.HTML);
              } else {
                return ok(summaryView.render(params.build()));
              }
            },
            classLoaderExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof SecurityException) {
                  return redirectToHome();
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
  public CompletionStage<Result> review(Request request, long programId) {
    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return CompletableFuture.completedFuture(redirectToHome());
    }
    return reviewWithApplicantId(
        request,
        applicantId.orElseThrow(() -> new MissingOptionalException(Long.class)),
        programId);
  }

  /**
   * Handles application submission. For applicants, submits the application. For admins previewing
   * the program, does not submit the application and simply redirects to the program page.
   */
  @Secure
  public CompletionStage<Result> submitWithApplicantId(
      Request request, long applicantId, long programId) {
    Optional<CiviFormProfile> submittingProfile = profileUtils.currentUserProfile(request);

    // If the user isn't already logged in within their browser session, send them home.
    if (submittingProfile.isEmpty()) {
      return CompletableFuture.completedFuture(redirectToHome());
    }

    if (submittingProfile.get().isCiviFormAdmin()) {
      return CompletableFuture.completedFuture(
          redirect(controllers.admin.routes.AdminProgramPreviewController.back(programId).url()));
    }

    return checkApplicantAuthorization(request, applicantId)
        .thenComposeAsync(v -> checkProgramAuthorization(request, programId))
        .thenComposeAsync(
            v -> submitInternal(request, applicantId, programId),
            classLoaderExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof SecurityException) {
                  return redirectToHome();
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }

  /**
   * Handles application submission. For applicants, submits the application. For admins previewing
   * the program, does not submit the application and simply redirects to the program page.
   */
  @Secure
  public CompletionStage<Result> submit(Request request, long programId) {
    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return CompletableFuture.completedFuture(redirectToHome());
    }
    return submitWithApplicantId(
        request,
        applicantId.orElseThrow(() -> new MissingOptionalException(Long.class)),
        programId);
  }

  /** Returns true if eligibility is gating and the application is ineligible, false otherwise. */
  private boolean shouldShowNotEligibleBanner(
      ReadOnlyApplicantProgramService roApplicantProgramService, long programId)
      throws ProgramNotFoundException {
    if (!programService.getFullProgramDefinition(programId).eligibilityIsGating()) {
      return false;
    }
    return roApplicantProgramService.isApplicationNotEligible();
  }

  private ApplicantProgramSummaryView.Params.Builder generateParamsBuilder(
      ReadOnlyApplicantProgramService roApplicantProgramService) {
    ImmutableList<AnswerData> summaryData = roApplicantProgramService.getSummaryDataOnlyActive();
    int totalBlockCount = roApplicantProgramService.getAllActiveBlocks().size();
    int completedBlockCount = roApplicantProgramService.getActiveAndCompletedInProgramBlockCount();
    String programTitle = roApplicantProgramService.getProgramTitle();

    return ApplicantProgramSummaryView.Params.builder()
        .setCompletedBlockCount(completedBlockCount)
        .setProgramTitle(programTitle)
        .setProgramType(roApplicantProgramService.getProgramType())
        .setSummaryData(summaryData)
        .setTotalBlockCount(totalBlockCount);
  }

  private CompletionStage<Result> submitInternal(
      Request request, long applicantId, long programId) {
    CiviFormProfile submittingProfile = profileUtils.currentUserProfile(request).orElseThrow();

    CompletableFuture<ApplicationModel> submitAppFuture =
        applicantService
            .submitApplication(applicantId, programId, submittingProfile, request)
            .toCompletableFuture();
    CompletableFuture<ReadOnlyApplicantProgramService> readOnlyApplicantProgramServiceFuture =
        applicantService
            .getReadOnlyApplicantProgramService(applicantId, programId)
            .toCompletableFuture();
    return CompletableFuture.allOf(readOnlyApplicantProgramServiceFuture, submitAppFuture)
        .thenApplyAsync(
            (v) -> {
              ApplicationModel application = submitAppFuture.join();
              Long applicationId = application.id;
              Call endOfProgramSubmission =
                  routes.UpsellController.considerRegister(
                      applicantId,
                      programId,
                      applicationId,
                      applicantRoutes.index(submittingProfile, applicantId).url());
              return found(endOfProgramSubmission);
            },
            classLoaderExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof ApplicationSubmissionException) {
                  Call reviewPage =
                      applicantRoutes.review(submittingProfile, applicantId, programId);
                  String errorMsg =
                      messagesApi
                          .preferred(request)
                          .at(MessageKey.BANNER_ERROR_SAVING_APPLICATION.getKeyName());
                  return found(reviewPage).flashing("banner", errorMsg);
                }
                if (cause instanceof ApplicationOutOfDateException) {
                  String errorMsg =
                      messagesApi
                          .preferred(request)
                          .at(MessageKey.TOAST_APPLICATION_OUT_OF_DATE.getKeyName());
                  Call reviewPage =
                      applicantRoutes.review(submittingProfile, applicantId, programId);
                  return redirect(reviewPage).flashing("error", errorMsg);
                }
                if (cause instanceof ApplicationNotEligibleException) {
                  ReadOnlyApplicantProgramService roApplicantProgramService =
                      readOnlyApplicantProgramServiceFuture.join();

                  try {
                    ProgramDefinition programDefinition =
                        programService.getFullProgramDefinition(programId);

                    return ok(
                        ineligibleBlockView.render(
                            request,
                            submittingProfile,
                            roApplicantProgramService,
                            messagesApi.preferred(request),
                            applicantId,
                            programDefinition));
                  } catch (ProgramNotFoundException e) {
                    notFound(e.toString());
                  }
                }
                if (cause instanceof DuplicateApplicationException) {
                  ReadOnlyApplicantProgramService roApplicantProgramService =
                      readOnlyApplicantProgramServiceFuture.join();
                  return ok(
                      preventDuplicateSubmissionView.render(
                          request,
                          roApplicantProgramService,
                          messagesApi.preferred(request),
                          applicantId,
                          profileUtils.currentUserProfileOrThrow(request)));
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }
}
