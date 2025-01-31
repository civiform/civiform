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
import services.program.ProgramService;
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

  public CompletionStage<Result> showProgramWithApplicantId(
      CiviFormController controller,
      Http.Request request,
      String programSlug,
      Long applicantId,
      CiviFormProfile profile) {
    return getProgramVersionForApplicant(applicantId, programSlug, request)
        .thenComposeAsync(
            (Optional<ProgramDefinition> programForExistingApplication) -> {
              CompletionStage<ProgramDefinition> programDefinitionStage;
              // Check to see if the applicant already has an application
              // for this program, redirect to program version associated
              // with that application if so.
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
                              activeProgramDefinition))
                  .exceptionally(
                      ex ->
                          controller
                              .notFound(ex.getMessage())
                              .removingFromSession(request, REDIRECT_TO_SESSION_KEY));
            },
            classLoaderExecutionContext.current());
  }

  private Result redirectToOverviewOrReviewPage(
      CiviFormController controller,
      Http.Request request,
      String programSlug,
      CiviFormProfile profile,
      long applicantId,
      ProgramDefinition activeProgramDefinition) {
    CompletableFuture<ApplicantPersonalInfo> applicantPersonalInfo =
        applicantService.getPersonalInfo(applicantId).toCompletableFuture();

    Optional<Boolean> optionalIsEligible =
        applicantService.getApplicantMayBeEligibleStatus(
            profile.getApplicant().join(), activeProgramDefinition);

    return settingsManifest.getNorthStarApplicantUi(request)
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
                    optionalIsEligible))
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

  private CompletionStage<Optional<ProgramDefinition>> getProgramVersionForApplicant(
      long applicantId, String programSlug, Http.Request request) {
    // Find all applicant's DRAFT applications for programs of the same slug
    // redirect to the newest program version with a DRAFT application.
    CiviFormProfile requesterProfile = profileUtils.currentUserProfile(request);
    return applicantService
        .relevantProgramsForApplicant(applicantId, requesterProfile, request)
        .thenApplyAsync(
            (ApplicationPrograms relevantPrograms) ->
                relevantPrograms.inProgress().stream()
                    .map(ApplicantProgramData::program)
                    .filter(program -> program.slug().equals(programSlug))
                    .findFirst(),
            classLoaderExecutionContext.current());
  }
}
