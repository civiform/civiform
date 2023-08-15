package controllers.applicant;

import static auth.DefaultToGuestRedirector.createGuestSessionAndRedirect;
import static com.google.common.base.Preconditions.checkNotNull;
import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import controllers.CiviFormController;
import controllers.LanguageUtils;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Applicant;
import models.DisplayMode;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.MessageKey;
import services.applicant.ApplicantService;
import services.applicant.ApplicantService.ApplicantProgramData;
import services.applicant.ApplicantService.ApplicationPrograms;
import services.program.ProgramDefinition;
import services.program.ProgramService;

/**
 * Controller for handling methods for deep links. Applicants will be asked to sign-in before they
 * can access the page.
 */
public final class DeepLinkController extends CiviFormController {

  private final HttpExecutionContext httpContext;
  private final ApplicantService applicantService;
  private final ProgramService programService;
  private final LanguageUtils languageUtils;
  private final MessagesApi messagesApi;

  @Inject
  public DeepLinkController(
      HttpExecutionContext httpContext,
      ApplicantService applicantService,
      ProfileUtils profileUtils,
      ProgramService programService,
      VersionRepository versionRepository,
      LanguageUtils languageUtils,
      MessagesApi messagesApi) {
    super(profileUtils, versionRepository);
    this.httpContext = checkNotNull(httpContext);
    this.applicantService = checkNotNull(applicantService);
    this.programService = checkNotNull(programService);
    this.languageUtils = checkNotNull(languageUtils);
    this.messagesApi = checkNotNull(messagesApi);
  }

  public CompletionStage<Result> programBySlug(Http.Request request, String programSlug) {
    Optional<CiviFormProfile> profile = profileUtils.currentUserProfile(request);

    if (profile.isEmpty()) {
      return CompletableFuture.completedFuture(createGuestSessionAndRedirect(request));
    }

    return profile
        .get()
        .getApplicant()
        .thenComposeAsync(
            (Applicant applicant) -> {
              // Attempt to set default language for the applicant.
              applicant = languageUtils.maybeSetDefaultLocale(applicant);
              final long applicantId = applicant.id;

              // If the applicant has not yet set their preferred language, redirect to
              // the information controller to ask for preferred language.
              if (!applicant.getApplicantData().hasPreferredLocale()) {
                return CompletableFuture.completedFuture(
                    redirect(
                            controllers.applicant.routes.ApplicantInformationController
                                .setLangFromBrowser(applicantId))
                        .withSession(
                            request.session().adding(REDIRECT_TO_SESSION_KEY, request.uri())));
              }

              Messages messages = messagesApi.preferred(request);
              return getProgramVersionForApplicant(applicantId, programSlug, request)
                  .thenComposeAsync(
                      (Optional<ProgramDefinition> programForExistingApplication) -> {
                        if (programForExistingApplication.isPresent()
                            && programForExistingApplication
                                .get()
                                .displayMode()
                                .equals(DisplayMode.DISABLED)) {
                          return CompletableFuture.completedFuture(
                              notFound(messages.at(MessageKey.PROGRAM_DISABLED.getKeyName())));
                        }
                        // Check to see if the applicant already has an application
                        // for this program, redirect to program version associated
                        // with that application if so.
                        if (programForExistingApplication.isPresent()) {
                          long programId = programForExistingApplication.get().id();
                          return CompletableFuture.completedFuture(
                              redirectToReviewPage(programId, applicantId, programSlug, request));
                        } else {
                          return programService
                              .getActiveProgramDefinitionAsync(programSlug)
                              .thenApply(
                                  activeProgramDefinition ->
                                      redirectToReviewPage(
                                          activeProgramDefinition.id(),
                                          applicantId,
                                          programSlug,
                                          request))
                              .exceptionally(
                                  ex ->
                                      notFound(ex.getMessage())
                                          .removingFromSession(request, REDIRECT_TO_SESSION_KEY));
                        }
                      },
                      httpContext.current());
            },
            httpContext.current());
  }

  private Result redirectToReviewPage(
      long programId, long applicantId, String programSlug, Http.Request request) {
    return redirect(
            controllers.applicant.routes.ApplicantProgramReviewController.review(
                applicantId, programId))
        .flashing("redirected-from-program-slug", programSlug)
        // If we had a redirectTo session key that redirected us here, remove it so that it doesn't
        // get used again.
        .removingFromSession(request, REDIRECT_TO_SESSION_KEY);
  }

  private CompletionStage<Optional<ProgramDefinition>> getProgramVersionForApplicant(
      long applicantId, String programSlug, Http.Request request) {
    // Find all applicant's DRAFT applications for programs of the same slug
    // redirect to the newest program version with a DRAFT application.
    CiviFormProfile requesterProfile = profileUtils.currentUserProfile(request).orElseThrow();
    return applicantService
        .relevantProgramsForApplicant(applicantId, requesterProfile)
        .thenApplyAsync(
            (ApplicationPrograms relevantPrograms) ->
                relevantPrograms.inProgress().stream()
                    .map(ApplicantProgramData::program)
                    .filter(program -> program.slug().equals(programSlug))
                    .findFirst(),
            httpContext.current());
  }
}
