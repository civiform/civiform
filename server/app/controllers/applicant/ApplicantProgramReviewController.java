package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static views.applicant.AuthenticateUpsellCreator.createLoginPromptModal;
import static views.components.Modal.RepeatOpenBehavior.Group.PROGRAM_SLUG_LOGIN_PROMPT;

import actions.ProgramDisabledAction;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import auth.controllers.MissingOptionalException;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import controllers.FlashKey;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.ApplicationModel;
import org.pac4j.play.java.Secure;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Call;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.With;
import repository.VersionRepository;
import services.AlertSettings;
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
import views.applicant.NorthStarApplicantIneligibleView;
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
@With(ProgramDisabledAction.class)
public class ApplicantProgramReviewController extends CiviFormController {

  private final ApplicantService applicantService;
  private final ClassLoaderExecutionContext classLoaderExecutionContext;
  private final MessagesApi messagesApi;
  private final ApplicantProgramSummaryView summaryView;
  private final NorthStarApplicantProgramSummaryView northStarSummaryView;
  private final NorthStarApplicantIneligibleView northStarApplicantIneligibleView;
  private final IneligibleBlockView ineligibleBlockView;
  private final PreventDuplicateSubmissionView preventDuplicateSubmissionView;
  private final SettingsManifest settingsManifest;
  private final ProgramService programService;
  private final ApplicantRoutes applicantRoutes;
  private final EligibilityAlertSettingsCalculator eligibilityAlertSettingsCalculator;

  @Inject
  public ApplicantProgramReviewController(
      ApplicantService applicantService,
      ClassLoaderExecutionContext classLoaderExecutionContext,
      MessagesApi messagesApi,
      ApplicantProgramSummaryView summaryView,
      NorthStarApplicantProgramSummaryView northStarSummaryView,
      NorthStarApplicantIneligibleView northStarApplicantIneligibleView,
      IneligibleBlockView ineligibleBlockView,
      PreventDuplicateSubmissionView preventDuplicateSubmissionView,
      ProfileUtils profileUtils,
      SettingsManifest settingsManifest,
      ProgramService programService,
      VersionRepository versionRepository,
      ApplicantRoutes applicantRoutes,
      EligibilityAlertSettingsCalculator eligibilityAlertSettingsCalculator) {
    super(profileUtils, versionRepository);
    this.applicantService = checkNotNull(applicantService);
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.messagesApi = checkNotNull(messagesApi);
    this.summaryView = checkNotNull(summaryView);
    this.northStarSummaryView = checkNotNull(northStarSummaryView);
    this.northStarApplicantIneligibleView = checkNotNull(northStarApplicantIneligibleView);
    this.ineligibleBlockView = checkNotNull(ineligibleBlockView);
    this.preventDuplicateSubmissionView = checkNotNull(preventDuplicateSubmissionView);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.programService = checkNotNull(programService);
    this.applicantRoutes = checkNotNull(applicantRoutes);
    this.eligibilityAlertSettingsCalculator = checkNotNull(eligibilityAlertSettingsCalculator);
  }

  @Secure
  public CompletionStage<Result> reviewWithApplicantId(
      Request request, long applicantId, long programId) {
    CiviFormProfile submittingProfile = profileUtils.currentUserProfile(request);

    Optional<String> flashBannerMessage = request.flash().get(FlashKey.BANNER);
    Optional<ToastMessage> flashBanner = flashBannerMessage.map(m -> ToastMessage.alert(m));
    Optional<String> flashSuccessBannerMessage = request.flash().get(FlashKey.SUCCESS_BANNER);
    Optional<ToastMessage> flashSuccessBanner =
        flashSuccessBannerMessage.map(m -> ToastMessage.success(m));

    CompletionStage<ApplicantPersonalInfo> applicantStage =
        applicantService.getPersonalInfo(applicantId);

    return applicantStage
        .thenComposeAsync(v -> checkApplicantAuthorization(request, applicantId))
        .thenComposeAsync(v -> checkProgramAuthorization(request, programId))
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            classLoaderExecutionContext.current())
        .thenApplyAsync(
            (roApplicantProgramService) -> {
              CiviFormProfile profile = profileUtils.currentUserProfile(request);

              Optional<Result> applicationUpdatedOptional =
                  updateApplicationToLatestProgramVersionIfNeeded(
                      request, applicantId, programId, profile);
              if (applicationUpdatedOptional.isPresent()) {
                return applicationUpdatedOptional.get();
              }

              Messages messages = messagesApi.preferred(request);

              AlertSettings eligibilityAlertSettings = AlertSettings.empty();

              if (roApplicantProgramService.shouldDisplayEligibilityMessage()) {
                eligibilityAlertSettings =
                    eligibilityAlertSettingsCalculator.calculate(
                        request,
                        profileUtils.currentUserProfile(request).isTrustedIntermediary(),
                        !roApplicantProgramService.isApplicationNotEligible(),
                        settingsManifest.getNorthStarApplicantUi(request),
                        false,
                        programId,
                        roApplicantProgramService.getIneligibleQuestions());
              }

              ApplicantProgramSummaryView.Params.Builder params =
                  this.generateParamsBuilder(roApplicantProgramService)
                      .setApplicantId(applicantId)
                      .setApplicantPersonalInfo(applicantStage.toCompletableFuture().join())
                      .setBannerMessages(ImmutableList.of(flashBanner, flashSuccessBanner))
                      .setEligibilityAlertSettings(eligibilityAlertSettings)
                      .setMessages(messages)
                      .setProgramId(programId)
                      .setRequest(request)
                      .setProfile(submittingProfile);

              // Show a login prompt on the review page if we were redirected from a program slug
              // and user is a guest.
              if (request.flash().get(FlashKey.REDIRECTED_FROM_PROGRAM_SLUG).isPresent()
                  && applicantStage.toCompletableFuture().join().getType() == ApplicantType.GUEST) {
                Modal loginPromptModal =
                    createLoginPromptModal(
                            messages,
                            /* postLoginRedirectTo= */ routes.ApplicantProgramsController.show(
                                    request
                                        .flash()
                                        .get(FlashKey.REDIRECTED_FROM_PROGRAM_SLUG)
                                        .get())
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
                int completedBlockCount =
                    roApplicantProgramService.getActiveAndCompletedInProgramBlockCount();
                ImmutableList<AnswerData> summaryData =
                    roApplicantProgramService.getSummaryDataOnlyActive();

                NorthStarApplicantProgramSummaryView.Params northStarParams =
                    NorthStarApplicantProgramSummaryView.Params.builder()
                        .setProgramTitle(roApplicantProgramService.getProgramTitle())
                        .setProgramShortDescription(
                            roApplicantProgramService.getProgramShortDescription())
                        .setBlocks(roApplicantProgramService.getAllActiveBlocks())
                        .setApplicantId(applicantId)
                        .setApplicantPersonalInfo(applicantStage.toCompletableFuture().join())
                        .setProfile(submittingProfile)
                        .setProgramId(programId)
                        .setCompletedBlockCount(completedBlockCount)
                        .setTotalBlockCount(totalBlockCount)
                        .setMessages(messages)
                        .setAlertBannerMessage(flashBannerMessage)
                        .setSuccessBannerMessage(flashSuccessBannerMessage)
                        .setEligibilityAlertSettings(eligibilityAlertSettings)
                        .setSummaryData(summaryData)
                        .setProgramType(roApplicantProgramService.getProgramType())
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
    CiviFormProfile submittingProfile = profileUtils.currentUserProfile(request);
    if (submittingProfile.isCiviFormAdmin()) {
      return CompletableFuture.completedFuture(
          redirect(controllers.admin.routes.AdminProgramPreviewController.back(programId).url()));
    }

    return checkApplicantAuthorization(request, applicantId)
        .thenComposeAsync(v -> checkProgramAuthorization(request, programId))
        .thenComposeAsync(
            v -> {
              CiviFormProfile profile = profileUtils.currentUserProfile(request);

              Optional<Result> applicationUpdatedOptional =
                  updateApplicationToLatestProgramVersionIfNeeded(
                      request, applicantId, programId, profile);
              if (applicationUpdatedOptional.isPresent()) {
                return CompletableFuture.completedFuture(applicationUpdatedOptional.get());
              }

              return submitInternal(request, applicantId, programId);
            },
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
    CiviFormProfile submittingProfile = profileUtils.currentUserProfile(request);
    CompletableFuture<ApplicationModel> submitAppFuture =
        applicantService
            .submitApplication(applicantId, programId, submittingProfile, request)
            .toCompletableFuture();

    CompletableFuture<ReadOnlyApplicantProgramService> readOnlyApplicantProgramServiceFuture =
        applicantService
            .getReadOnlyApplicantProgramService(applicantId, programId)
            .toCompletableFuture();
    CompletableFuture<ApplicantPersonalInfo> applicantPersonalInfo =
        applicantService.getPersonalInfo(applicantId).toCompletableFuture();
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
                      applicantRoutes.index(submittingProfile, applicantId).url(),
                      application.getSubmitTime().toString());
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
                  return found(reviewPage).flashing(FlashKey.BANNER, errorMsg);
                }
                if (cause instanceof ApplicationOutOfDateException) {
                  String errorMsg =
                      messagesApi
                          .preferred(request)
                          .at(MessageKey.TOAST_APPLICATION_OUT_OF_DATE.getKeyName());
                  Call reviewPage =
                      applicantRoutes.review(submittingProfile, applicantId, programId);
                  return redirect(reviewPage).flashing(FlashKey.ERROR, errorMsg);
                }
                if (cause instanceof ApplicationNotEligibleException) {
                  ReadOnlyApplicantProgramService roApplicantProgramService =
                      readOnlyApplicantProgramServiceFuture.join();

                  try {
                    ProgramDefinition programDefinition =
                        programService.getFullProgramDefinition(programId);
                    return renderIneligiblePage(
                        request,
                        submittingProfile,
                        applicantId,
                        applicantPersonalInfo.join(),
                        roApplicantProgramService,
                        programDefinition);
                  } catch (ProgramNotFoundException e) {
                    notFound(e.toString());
                  }
                }
                if (cause instanceof DuplicateApplicationException) {
                  return renderPreventDuplicateSubmissionPage(
                      request,
                      submittingProfile,
                      applicantId,
                      readOnlyApplicantProgramServiceFuture.join(),
                      programId);
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }

  // TODO(#7266): Delete the old codepath and inline the North Star path
  private Result renderIneligiblePage(
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
      return ok(northStarApplicantIneligibleView.render(params)).as(Http.MimeTypes.HTML);
    } else {
      return ok(
          ineligibleBlockView.render(
              request,
              profile,
              roApplicantProgramService,
              messagesApi.preferred(request),
              applicantId,
              programDefinition,
              /* blockDefinition= */ Optional.empty()));
    }
  }

  // TODO(#7266): Delete the old codepath and inline the North Star path
  private Result renderPreventDuplicateSubmissionPage(
      Request request,
      CiviFormProfile profile,
      long applicantId,
      ReadOnlyApplicantProgramService roApplicantProgramService,
      long programId) {
    if (settingsManifest.getNorthStarApplicantUi(request)) {
      Call reviewPage = applicantRoutes.review(profile, applicantId, programId);
      return found(reviewPage).flashing(FlashKey.DUPLICATE_SUBMISSION, "true");
    } else {
      return ok(
          preventDuplicateSubmissionView.render(
              request,
              roApplicantProgramService,
              messagesApi.preferred(request),
              applicantId,
              profileUtils.currentUserProfile(request)));
    }
  }

  /**
   * Check if the application needs to be updated to a newer program version. If it does, update and
   * return a redirect result back to the review page
   *
   * @return {@link Result} if application was updated; empty if not
   */
  public Optional<Result> updateApplicationToLatestProgramVersionIfNeeded(
      Http.Request request, long applicantId, long programId, CiviFormProfile profile) {
    if (!settingsManifest.getFastforwardEnabled(request)) {
      return Optional.empty();
    }

    return applicantService
        .updateApplicationToLatestProgramVersion(applicantId, programId)
        .map(
            latestProgramId ->
                redirect(applicantRoutes.review(profile, applicantId, latestProgramId).url())
                    .flashing(FlashKey.SHOW_FAST_FORWARDED_MESSAGE, "true"));
  }
}
