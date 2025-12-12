package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import actions.ProgramDisabledAction;
import auth.Authorizers;
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
import org.apache.commons.lang3.StringUtils;
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
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applicant.exception.ApplicationNotEligibleException;
import services.applicant.exception.ApplicationOutOfDateException;
import services.applicant.exception.ApplicationSubmissionException;
import services.applicant.exception.DuplicateApplicationException;
import services.monitoring.MonitoringMetricCounters;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import views.applicant.ApplicantIneligibleView;
import views.applicant.ApplicantProgramSummaryView;

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
  private final ApplicantIneligibleView applicantIneligibleView;
  private final SettingsManifest settingsManifest;
  private final ProgramService programService;
  private final ProgramSlugHandler programSlugHandler;
  private final ApplicantRoutes applicantRoutes;
  private final EligibilityAlertSettingsCalculator eligibilityAlertSettingsCalculator;
  private final MonitoringMetricCounters metricCounters;

  @Inject
  public ApplicantProgramReviewController(
      ApplicantService applicantService,
      ClassLoaderExecutionContext classLoaderExecutionContext,
      MessagesApi messagesApi,
      ApplicantProgramSummaryView summaryView,
      ApplicantIneligibleView applicantIneligibleView,
      ProfileUtils profileUtils,
      SettingsManifest settingsManifest,
      ProgramService programService,
      VersionRepository versionRepository,
      ProgramSlugHandler programSlugHandler,
      ApplicantRoutes applicantRoutes,
      EligibilityAlertSettingsCalculator eligibilityAlertSettingsCalculator,
      MonitoringMetricCounters metricCounters) {
    super(profileUtils, versionRepository);
    this.applicantService = checkNotNull(applicantService);
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.messagesApi = checkNotNull(messagesApi);
    this.summaryView = checkNotNull(summaryView);
    this.applicantIneligibleView = checkNotNull(applicantIneligibleView);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.programService = checkNotNull(programService);
    this.programSlugHandler = checkNotNull(programSlugHandler);
    this.applicantRoutes = checkNotNull(applicantRoutes);
    this.eligibilityAlertSettingsCalculator = checkNotNull(eligibilityAlertSettingsCalculator);
    this.metricCounters = checkNotNull(metricCounters);
  }

  /**
   * Renders the application review page for TIs applying on behalf of clients and CiviForm admins
   * previewing programs.
   */
  @Secure(authorizers = Authorizers.Labels.TI_OR_CIVIFORM_ADMIN)
  public CompletionStage<Result> reviewWithApplicantId(
      Request request, long applicantId, String programParam, Boolean isFromUrlCall) {
    // Redirect home when the program param is the program id (numeric) but it should be the program
    // slug because the program slug URL is enabled and it comes from the URL call
    boolean programSlugUrlEnabled = settingsManifest.getProgramSlugUrlsEnabled(request);
    if (programSlugUrlEnabled && isFromUrlCall && StringUtils.isNumeric(programParam)) {
      metricCounters
          .getUrlWithProgramIdCall()
          .labels("/applicants/:applicantId/programs/:programParam/review", programParam)
          .inc();
      return CompletableFuture.completedFuture(redirectToHome());
    }
    return reviewInternal(request, applicantId, programParam, isFromUrlCall);
  }

  public CompletionStage<Result> reviewInternal(
      Request request, long applicantId, String programParam, Boolean isFromUrlCall) {
    boolean programSlugUrlEnabled = settingsManifest.getProgramSlugUrlsEnabled(request);
    return programSlugHandler
        .resolveProgramParam(programParam, applicantId, isFromUrlCall, programSlugUrlEnabled)
        .thenCompose(
            programId -> {
              CiviFormProfile submittingProfile = profileUtils.currentUserProfile(request);

              CompletionStage<ApplicantPersonalInfo> applicantStage =
                  applicantService.getPersonalInfo(applicantId);
              return applicantStage
                  .thenComposeAsync(v -> checkApplicantAuthorization(request, applicantId))
                  .thenComposeAsync(v -> checkProgramAuthorization(request, programId))
                  .thenComposeAsync(
                      v ->
                          applicantService.getReadOnlyApplicantProgramService(
                              applicantId, programId),
                      classLoaderExecutionContext.current())
                  .thenApplyAsync(
                      (roApplicantProgramService) -> {
                        CiviFormProfile profile = profileUtils.currentUserProfile(request);

                        Optional<Result> applicationUpdatedOptional =
                            updateApplicationToLatestProgramVersionIfNeeded(
                                applicantId, programId, profile);
                        if (applicationUpdatedOptional.isPresent()) {
                          return applicationUpdatedOptional.get();
                        }

                        Optional<String> flashBannerMessage = request.flash().get(FlashKey.BANNER);
                        Optional<String> flashSuccessBannerMessage =
                            request.flash().get(FlashKey.SUCCESS_BANNER);
                        Messages messages = messagesApi.preferred(request);

                        AlertSettings eligibilityAlertSettings = AlertSettings.empty();
                        if (roApplicantProgramService.shouldDisplayEligibilityMessage()) {
                          eligibilityAlertSettings =
                              eligibilityAlertSettingsCalculator.calculate(
                                  request,
                                  profileUtils.currentUserProfile(request).isTrustedIntermediary(),
                                  !roApplicantProgramService.isApplicationNotEligible(),
                                  false,
                                  programId,
                                  roApplicantProgramService.getIneligibleQuestions());
                        }

                        int totalBlockCount = roApplicantProgramService.getAllActiveBlocks().size();
                        int completedBlockCount =
                            roApplicantProgramService.getActiveAndCompletedInProgramBlockCount();
                        ImmutableList<AnswerData> summaryData =
                            roApplicantProgramService.getSummaryDataOnlyActive();

                        ApplicantProgramSummaryView.Params northStarParams =
                            ApplicantProgramSummaryView.Params.builder()
                                .setProgramTitle(roApplicantProgramService.getProgramTitle())
                                .setProgramShortDescription(
                                    roApplicantProgramService.getProgramShortDescription())
                                .setBlocks(roApplicantProgramService.getAllActiveBlocks())
                                .setApplicantId(applicantId)
                                .setApplicantPersonalInfo(
                                    applicantStage.toCompletableFuture().join())
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
                                .setLoginOnly(
                                    roApplicantProgramService.isProgramOnlyForLoggedInApplicants())
                                .build();
                        return ok(summaryView.render(request, northStarParams))
                            .as(Http.MimeTypes.HTML);
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
            });
  }

  @Secure(authorizers = Authorizers.Labels.APPLICANT)
  public CompletionStage<Result> review(
      Request request, String programParam, Boolean isFromUrlCall) {
    // Redirect home when the program param is the program id (numeric) but it should be the program
    // slug because the program slug URL is enabled and it comes from the URL call
    boolean programSlugUrlEnabled = settingsManifest.getProgramSlugUrlsEnabled(request);
    if (programSlugUrlEnabled && isFromUrlCall && StringUtils.isNumeric(programParam)) {
      metricCounters
          .getUrlWithProgramIdCall()
          .labels("/programs/:programParam/review", programParam)
          .inc();
      return CompletableFuture.completedFuture(redirectToHome());
    }

    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return CompletableFuture.completedFuture(redirectToHome());
    }

    return reviewInternal(request, applicantId.get(), programParam, isFromUrlCall);
  }

  /**
   * Handles application submission for TIs applying on behalf of clients and CiviForm admins
   * previewing programs.
   *
   * <p>Program Admins can't actually submit the application and are redirected to the program page.
   */
  @Secure(authorizers = Authorizers.Labels.TI_OR_CIVIFORM_ADMIN)
  public CompletionStage<Result> submitWithApplicantId(
      Request request, long applicantId, long programId) {
    CiviFormProfile submittingProfile = profileUtils.currentUserProfile(request);
    if (submittingProfile.isCiviFormAdmin()) {
      return CompletableFuture.completedFuture(
          redirect(controllers.admin.routes.AdminProgramPreviewController.back(programId).url()));
    }
    return submitInternalWithAuth(request, applicantId, programId);
  }

  private CompletionStage<Result> submitInternalWithAuth(
      Request request, long applicantId, long programId) {
    return checkApplicantAuthorization(request, applicantId)
        .thenComposeAsync(v -> checkProgramAuthorization(request, programId))
        .thenComposeAsync(
            v -> {
              CiviFormProfile profile = profileUtils.currentUserProfile(request);

              Optional<Result> applicationUpdatedOptional =
                  updateApplicationToLatestProgramVersionIfNeeded(applicantId, programId, profile);
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

  /** Handles application submission. */
  @Secure(authorizers = Authorizers.Labels.APPLICANT)
  public CompletionStage<Result> submit(Request request, long programId) {
    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return CompletableFuture.completedFuture(redirectToHome());
    }
    return submitInternalWithAuth(
        request,
        applicantId.orElseThrow(() -> new MissingOptionalException(Long.class)),
        programId);
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
                  Call reviewPage =
                      applicantRoutes.review(submittingProfile, applicantId, programId);
                  return found(reviewPage).flashing(FlashKey.DUPLICATE_SUBMISSION, "true");
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }

  private Result renderIneligiblePage(
      Request request,
      CiviFormProfile profile,
      long applicantId,
      ApplicantPersonalInfo personalInfo,
      ReadOnlyApplicantProgramService roApplicantProgramService,
      ProgramDefinition programDefinition) {
    ApplicantIneligibleView.Params params =
        ApplicantIneligibleView.Params.builder()
            .setRequest(request)
            .setApplicantId(applicantId)
            .setProfile(profile)
            .setApplicantPersonalInfo(personalInfo)
            .setProgramDefinition(programDefinition)
            .setRoApplicantProgramService(roApplicantProgramService)
            .setMessages(messagesApi.preferred(request))
            .build();
    return ok(applicantIneligibleView.render(params)).as(Http.MimeTypes.HTML);
  }

  /**
   * Check if the application needs to be updated to a newer program version. If it does, update and
   * return a redirect result back to the review page
   *
   * @return {@link Result} if application was updated; empty if not
   */
  public Optional<Result> updateApplicationToLatestProgramVersionIfNeeded(
      long applicantId, long programId, CiviFormProfile profile) {
    return applicantService
        .updateApplicationToLatestProgramVersion(applicantId, programId)
        .map(
            latestProgramId ->
                redirect(applicantRoutes.review(profile, applicantId, latestProgramId).url())
                    .flashing(FlashKey.SHOW_FAST_FORWARDED_MESSAGE, "true"));
  }
}
