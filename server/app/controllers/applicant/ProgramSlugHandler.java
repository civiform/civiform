package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import controllers.CiviFormController;
import controllers.FlashKey;
import controllers.LanguageUtils;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.ApplicantModel;
import models.DisplayMode;
import org.apache.commons.lang3.StringUtils;
import play.i18n.MessagesApi;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.applicant.ApplicantService.ApplicantProgramData;
import services.applicant.ApplicantService.ApplicationPrograms;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.ProgramType;
import services.settings.SettingsManifest;
import views.applicant.NorthStarProgramOverviewView;

/** Class for showing program view based on program slug. */
public final class ProgramSlugHandler {

  private final ClassLoaderExecutionContext classLoaderExecutionContext;
  private final ApplicantService applicantService;
  private final ProfileUtils profileUtils;
  private final ProgramService programService;
  private final LanguageUtils languageUtils;
  private final ApplicantRoutes applicantRoutes;
  private final SettingsManifest settingsManifest;
  private final NorthStarProgramOverviewView northStarProgramOverviewView;
  private final MessagesApi messagesApi;

  @Inject
  public ProgramSlugHandler(
      ClassLoaderExecutionContext classLoaderExecutionContext,
      ApplicantService applicantService,
      ProfileUtils profileUtils,
      ProgramService programService,
      LanguageUtils languageUtils,
      ApplicantRoutes applicantRoutes,
      SettingsManifest settingsManifest,
      NorthStarProgramOverviewView northStarProgramOverviewView,
      MessagesApi messagesApi) {
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.applicantService = checkNotNull(applicantService);
    this.profileUtils = checkNotNull(profileUtils);
    this.programService = checkNotNull(programService);
    this.languageUtils = checkNotNull(languageUtils);
    this.applicantRoutes = checkNotNull(applicantRoutes);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.northStarProgramOverviewView = checkNotNull(northStarProgramOverviewView);
    this.messagesApi = checkNotNull(messagesApi);
  }

  public CompletionStage<Result> showProgram(
      CiviFormController controller, Http.Request request, String programSlug) {
    CiviFormProfile profile = profileUtils.currentUserProfile(request);

    return profile
        .getApplicant()
        .thenComposeAsync(
            (ApplicantModel applicant) -> {
              // Attempt to set default language for the applicant.
              applicant = languageUtils.maybeSetDefaultLocale(applicant);
              final long applicantId = applicant.id;

              // If the applicant has not yet set their preferred language, redirect to
              // the information controller to ask for preferred language.
              if (!applicant.getApplicantData().hasPreferredLocale()) {
                return CompletableFuture.completedFuture(
                    controller
                        .redirect(
                            controllers.applicant.routes.ApplicantInformationController
                                .setLangFromBrowser(applicantId))
                        .withSession(
                            request.session().adding(REDIRECT_TO_SESSION_KEY, request.uri())));
              }

              return showProgramWithApplicantId(
                  controller, request, programSlug, applicantId, profile);
            },
            classLoaderExecutionContext.current());
  }

  public CompletionStage<Result> showProgramPreview(
      CiviFormController controller, Http.Request request, String programSlug) {
    CiviFormProfile profile = profileUtils.currentUserProfile(request);

    return profile
        .getApplicant()
        .thenComposeAsync(
            (ApplicantModel applicant) -> {
              CompletionStage<ProgramDefinition> programDefinitionStage =
                  programService.getActiveOrDraftFullProgramDefinitionAsync(programSlug);
              return programDefinitionStage
                  .thenApply(
                      activeOrDraftProgramDefinition ->
                          redirectToOverviewOrReviewPage(
                              controller,
                              request,
                              programSlug,
                              profile,
                              applicant.id,
                              activeOrDraftProgramDefinition,
                              null))
                  .exceptionally(
                      ex ->
                          Results.notFound(ex.getMessage())
                              .removingFromSession(request, REDIRECT_TO_SESSION_KEY));
            },
            classLoaderExecutionContext.current());
  }

  public CompletionStage<Result> showProgramWithApplicantId(
      CiviFormController controller,
      Http.Request request,
      String programSlug,
      Long applicantId,
      CiviFormProfile profile) {
    return applicantService
        .relevantProgramsForApplicant(applicantId, profile, request)
        .thenComposeAsync(
            (ApplicationPrograms relevantPrograms) -> {
              // Check to see if the applicant already has an application
              // for this program, redirect to program version associated
              // with that application if so.
              Optional<ProgramDefinition> programForExistingApplication =
                  relevantPrograms.inProgress().stream()
                      .map(ApplicantProgramData::program)
                      .filter(program -> program.slug().equals(programSlug))
                      .findFirst();

              CompletionStage<ProgramDefinition> programDefinitionStage;

              if (programForExistingApplication.isPresent()) {
                long programId = programForExistingApplication.get().id();
                programDefinitionStage = programService.getFullProgramDefinitionAsync(programId);
              } else {
                programDefinitionStage =
                    programService.getActiveFullProgramDefinitionAsync(programSlug);
              }
              return programDefinitionStage
                  .thenApply(
                      activeProgramDefinition ->
                          redirectToOverviewOrReviewPage(
                              controller,
                              request,
                              programSlug,
                              profile,
                              applicantId,
                              activeProgramDefinition,
                              relevantPrograms))
                  .exceptionally(
                      ex ->
                          controller
                              .notFound(ex.getMessage())
                              .removingFromSession(request, REDIRECT_TO_SESSION_KEY));
            },
            classLoaderExecutionContext.current());
  }

  /**
   * Resolves a program parameter to a program ID, handling both program slugs and numeric IDs.
   *
   * @param programParam The program parameter (either slug or numeric ID)
   * @param applicantId The applicant ID for slug resolution
   * @param isFromUrlCall Whether this call originated from a URL
   * @param programSlugUrlEnabled Whether program slug URLs feature is enabled
   * @return CompletionStage containing the resolved program ID
   */
  public CompletionStage<Long> resolveProgramParam(
      String programParam, Long applicantId, Boolean isFromUrlCall, Boolean programSlugUrlEnabled) {
    if (programSlugUrlEnabled && isFromUrlCall) {
      if (StringUtils.isNumeric(programParam)) {
        // This should have been previously handled by the caller, since we don't support program
        // ids (numeric) when feature is enabled and call comes directly from the URL
        throw new IllegalStateException(
            "Numeric program parameter should have been handled by the caller");
      }
      return getLatestProgramId(programParam, applicantId);
    }

    try {
      Long programId = Long.parseLong(programParam);
      return CompletableFuture.completedFuture(programId);
    } catch (NumberFormatException e) {
      throw new RuntimeException(
          String.format("Could not parse value from '%s' to a numeric value", programParam));
    }
  }

  /**
   * Returns the program ID from the applicant's latest application if one exists, otherwise returns
   * the currently active program version ID.
   */
  public CompletionStage<Long> getLatestProgramId(String programSlug, long applicantId) {
    return applicantService
        .getLatestProgramId(programSlug, applicantId)
        .thenCompose(
            programId -> {
              return programId.isPresent()
                  ? CompletableFuture.completedFuture(programId.get())
                  : programService.getActiveProgramId(programSlug);
            });
  }

  private Result redirectToOverviewOrReviewPage(
      CiviFormController controller,
      Http.Request request,
      String programSlug,
      CiviFormProfile profile,
      long applicantId,
      ProgramDefinition activeProgramDefinition,
      ApplicationPrograms relevantPrograms) {
    // External programs don't have an overview or review page
    if (activeProgramDefinition.programType().equals(ProgramType.EXTERNAL)) {
      return Results.badRequest(new ProgramNotFoundException(programSlug).getMessage());
    }

    // For pre-screener forms, redirect to the first block edit page
    if (activeProgramDefinition.programType().equals(ProgramType.COMMON_INTAKE_FORM)) {
      return Results.redirect(
              applicantRoutes.edit(profile, applicantId, activeProgramDefinition.id()))
          .flashing(FlashKey.REDIRECTED_FROM_PROGRAM_SLUG, programSlug)
          // If we had a redirectTo session key that redirected us here, remove it so that it
          // doesn't get used again.
          .removingFromSession(request, REDIRECT_TO_SESSION_KEY);
    }

    CompletableFuture<ApplicantPersonalInfo> applicantPersonalInfo =
        applicantService.getPersonalInfo(applicantId).toCompletableFuture();

    Optional<ApplicantProgramData> optionalProgramData = Optional.empty();

    if (relevantPrograms != null) {
      // If the program doesn't have any applications yet, find the program data
      // for the program that we're trying to show so that we can check isProgramMaybeEligible.
      optionalProgramData =
          relevantPrograms.unapplied().stream()
              .filter(
                  (ApplicantProgramData applicantProgramData) ->
                      applicantProgramData.programId() == activeProgramDefinition.id())
              .findFirst();
    }

    // TODO(#11582): North star clean up
    return settingsManifest.getNorthStarApplicantUi()
            && activeProgramDefinition.displayMode()
                != DisplayMode.DISABLED // If the program is disabled,
        // redirect to review page because that will trigger the ProgramDisabledAction.
        ? Results.ok(
                northStarProgramOverviewView.render(
                    messagesApi.preferred(request),
                    request,
                    applicantId,
                    applicantPersonalInfo.join(),
                    profile,
                    activeProgramDefinition,
                    optionalProgramData))
            .as("text/html")
            .removingFromSession(request, REDIRECT_TO_SESSION_KEY)
        : redirectToReviewPage(
            controller, activeProgramDefinition.id(), applicantId, programSlug, request, profile);
  }

  private Result redirectToReviewPage(
      CiviFormController controller,
      long programId,
      long applicantId,
      String programSlug,
      Http.Request request,
      CiviFormProfile profile) {
    return controller
        .redirect(applicantRoutes.review(profile, applicantId, programId))
        .flashing(FlashKey.REDIRECTED_FROM_PROGRAM_SLUG, programSlug)
        // If we had a redirectTo session key that redirected us here, remove it so that it doesn't
        // get used again.
        .removingFromSession(request, REDIRECT_TO_SESSION_KEY);
  }
}
