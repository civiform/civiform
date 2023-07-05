package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static views.applicant.AuthenticateUpsellCreator.createLoginPromptModal;
import static views.components.Modal.RepeatOpenBehavior;
import static views.components.Modal.RepeatOpenBehavior.Group.PROGRAM_SLUG_LOGIN_PROMPT;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Application;
import org.pac4j.play.java.Secure;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Call;
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
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import views.applicant.ApplicantProgramSummaryView;
import views.applicant.IneligibleBlockView;
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
  private final HttpExecutionContext httpExecutionContext;
  private final MessagesApi messagesApi;
  private final ApplicantProgramSummaryView summaryView;
  private final IneligibleBlockView ineligibleBlockView;
  private final SettingsManifest settingsManifest;
  private final ProgramService programService;

  @Inject
  public ApplicantProgramReviewController(
      ApplicantService applicantService,
      HttpExecutionContext httpExecutionContext,
      MessagesApi messagesApi,
      ApplicantProgramSummaryView summaryView,
      IneligibleBlockView ineligibleBlockView,
      ProfileUtils profileUtils,
      SettingsManifest settingsManifest,
      ProgramService programService,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.applicantService = checkNotNull(applicantService);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.messagesApi = checkNotNull(messagesApi);
    this.summaryView = checkNotNull(summaryView);
    this.ineligibleBlockView = checkNotNull(ineligibleBlockView);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.programService = checkNotNull(programService);
  }

  public CompletionStage<Result> review(Request request, long applicantId, long programId) {
    CiviFormProfile submittingProfile = profileUtils.currentUserProfile(request).orElseThrow();
    boolean isTrustedIntermediary = submittingProfile.isTrustedIntermediary();
    Optional<ToastMessage> flashBanner =
        request.flash().get("banner").map(m -> ToastMessage.alert(m));
    Optional<ToastMessage> flashSuccessBanner =
        request.flash().get("success-banner").map(m -> ToastMessage.success(m));
    CompletionStage<ApplicantPersonalInfo> applicantStage =
        applicantService.getPersonalInfo(applicantId);

    return applicantStage
        .thenComposeAsync(v -> checkApplicantAuthorization(request, applicantId))
        .thenComposeAsync(v -> checkProgramAuthorization(request, programId))
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId),
            httpExecutionContext.current())
        .thenApplyAsync(
            (roApplicantProgramService) -> {
              Messages messages = messagesApi.preferred(request);
              Optional<ToastMessage> notEligibleBanner = Optional.empty();
              try {
                if (shouldShowNotEligibleBanner(request, roApplicantProgramService, programId)) {
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
                      .setRequest(request);

              // Show a login prompt on the review page if we were redirected from a program slug
              // and user is a guest.
              if (request.flash().get("redirected-from-program-slug").isPresent()
                  && applicantStage.toCompletableFuture().join().getType() == ApplicantType.GUEST) {
                Modal loginPromptModal =
                    createLoginPromptModal(
                            messages,
                            /*postLoginRedirectTo=*/ controllers.applicant.routes.RedirectController
                                .programBySlug(
                                    request.flash().get("redirected-from-program-slug").get())
                                .url(),
                            messages.at(
                                MessageKey.INITIAL_LOGIN_MODAL_PROMPT.getKeyName(),
                                // The applicant portal name should always be set (there is a
                                // default setting as well).
                                settingsManifest.getApplicantPortalName().orElse("")),
                            MessageKey.BUTTON_CONTINUE_TO_APPLICATION)
                        .setDisplayOnLoad(true)
                        .setRepeatOpenBehavior(
                            RepeatOpenBehavior.showOnlyOnce(PROGRAM_SLUG_LOGIN_PROMPT))
                        .build();
                params.setLoginPromptModal(loginPromptModal);
              }

              return ok(summaryView.render(params.build()));
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
   * Handles application submission. For applicants, submits the application. For admins previewing
   * the program, does not submit the application and simply redirects to the program page.
   */
  @Secure
  public CompletionStage<Result> submit(Request request, long applicantId, long programId) {
    if (profileUtils.currentUserProfile(request).orElseThrow().isCiviFormAdmin()) {
      return versionRepository
          .isDraftProgramAsync(programId)
          .thenApplyAsync(
              (isDraftProgram) -> {
                Call reviewPage =
                    controllers.admin.routes.AdminProgramBlocksController.readOnlyIndex(programId);
                if (isDraftProgram) {
                  reviewPage =
                      controllers.admin.routes.AdminProgramBlocksController.index(programId);
                }
                return redirect(reviewPage);
              });
    }

    return checkApplicantAuthorization(request, applicantId)
        .thenComposeAsync(v -> checkProgramAuthorization(request, programId))
        .thenComposeAsync(
            v -> submitInternal(request, applicantId, programId), httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof SecurityException) {
                  return unauthorized();
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }

  /** Returns true if eligibility is gating and the application is ineligible, false otherwise. */
  private boolean shouldShowNotEligibleBanner(
      Request request, ReadOnlyApplicantProgramService roApplicantProgramService, long programId)
      throws ProgramNotFoundException {
    if (settingsManifest.getNongatedEligibilityEnabled(request)
        && !programService.getProgramDefinition(programId).eligibilityIsGating()) {
      return false;
    }
    return roApplicantProgramService.isApplicationNotEligible();
  }

  private ApplicantProgramSummaryView.Params.Builder generateParamsBuilder(
      ReadOnlyApplicantProgramService roApplicantProgramService) {
    ImmutableList<AnswerData> summaryData = roApplicantProgramService.getSummaryData();
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

    CompletableFuture<Application> submitAppFuture =
        applicantService
            .submitApplication(
                applicantId,
                programId,
                submittingProfile,
                settingsManifest.getNongatedEligibilityEnabled(request))
            .toCompletableFuture();
    CompletableFuture<ReadOnlyApplicantProgramService> readOnlyApplicantProgramServiceFuture =
        applicantService
            .getReadOnlyApplicantProgramService(applicantId, programId)
            .toCompletableFuture();
    return CompletableFuture.allOf(readOnlyApplicantProgramServiceFuture, submitAppFuture)
        .thenApplyAsync(
            (v) -> {
              Application application = submitAppFuture.join();
              Long applicationId = application.id;
              Call endOfProgramSubmission =
                  routes.RedirectController.considerRegister(
                      applicantId,
                      programId,
                      applicationId,
                      routes.ApplicantProgramsController.index(applicantId).url());
              return found(endOfProgramSubmission);
            },
            httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof ApplicationSubmissionException) {
                  Call reviewPage =
                      routes.ApplicantProgramReviewController.review(applicantId, programId);
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
                      routes.ApplicantProgramReviewController.review(applicantId, programId);
                  return redirect(reviewPage).flashing("error", errorMsg);
                }
                if (cause instanceof ApplicationNotEligibleException) {
                  ReadOnlyApplicantProgramService roApplicantProgramService =
                      readOnlyApplicantProgramServiceFuture.join();

                  try {
                    ProgramDefinition programDefinition =
                        programService.getProgramDefinition(programId);

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
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }
}
