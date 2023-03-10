package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static views.components.ToastMessage.ToastType.ALERT;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import featureflags.FeatureFlags;
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
import services.MessageKey;
import services.applicant.AnswerData;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applicant.exception.ApplicationNotEligibleException;
import services.applicant.exception.ApplicationOutOfDateException;
import services.applicant.exception.ApplicationSubmissionException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.applicant.ApplicantProgramSummaryView;
import views.applicant.IneligibleBlockView;
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
  private final ProfileUtils profileUtils;
  private final FeatureFlags featureFlags;
  private final ProgramService programService;

  @Inject
  public ApplicantProgramReviewController(
      ApplicantService applicantService,
      HttpExecutionContext httpExecutionContext,
      MessagesApi messagesApi,
      ApplicantProgramSummaryView summaryView,
      IneligibleBlockView ineligibleBlockView,
      ProfileUtils profileUtils,
      FeatureFlags featureFlags,
      ProgramService programService) {
    this.applicantService = checkNotNull(applicantService);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.messagesApi = checkNotNull(messagesApi);
    this.summaryView = checkNotNull(summaryView);
    this.ineligibleBlockView = checkNotNull(ineligibleBlockView);
    this.profileUtils = checkNotNull(profileUtils);
    this.featureFlags = checkNotNull(featureFlags);
    this.programService = checkNotNull(programService);
  }

  public CompletionStage<Result> review(Request request, long applicantId, long programId) {
    CiviFormProfile submittingProfile = profileUtils.currentUserProfile(request).orElseThrow();
    boolean isTrustedIntermediary = submittingProfile.isTrustedIntermediary();
    Optional<ToastMessage> flashBanner =
        request.flash().get("banner").map(m -> new ToastMessage(m, ALERT));
    CompletionStage<Optional<String>> applicantStage = applicantService.getName(applicantId);

    return applicantStage
        .thenComposeAsync(v -> checkApplicantAuthorization(profileUtils, request, applicantId))
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
                          new ToastMessage(
                              messages.at(
                                  isTrustedIntermediary
                                      ? MessageKey.TOAST_MAY_NOT_QUALIFY_TI.getKeyName()
                                      : MessageKey.TOAST_MAY_NOT_QUALIFY.getKeyName(),
                                  roApplicantProgramService.getProgramTitle()),
                              ALERT));
                }
              } catch (ProgramNotFoundException e) {
                return notFound(e.toString());
              }
              ApplicantProgramSummaryView.Params params =
                  this.generateParamsBuilder(roApplicantProgramService)
                      .setApplicantId(applicantId)
                      .setApplicantName(applicantStage.toCompletableFuture().join())
                      .setBannerMessages(ImmutableList.of(flashBanner, notEligibleBanner))
                      .setMessages(messages)
                      .setProgramId(programId)
                      .setRequest(request)
                      .build();
              return ok(summaryView.render(params));
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
  public CompletionStage<Result> submit(Request request, long applicantId, long programId) {
    return checkApplicantAuthorization(profileUtils, request, applicantId)
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
    if (!featureFlags.isProgramEligibilityConditionsEnabled(request)) {
      return false;
    }
    if (featureFlags.isNongatedEligibilityEnabled(request)
        && !programService.getProgramDefinition(programId).eligibilityIsGating()) {
      return false;
    }
    return !roApplicantProgramService.isApplicationEligible();
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
                featureFlags.isProgramEligibilityConditionsEnabled(request),
                featureFlags.isNongatedEligibilityEnabled(request))
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
