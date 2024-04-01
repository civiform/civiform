package controllers.applicant;

import static auth.DefaultToGuestRedirector.createGuestSessionAndRedirect;
import static com.google.common.base.Preconditions.checkNotNull;
import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import auth.controllers.MissingOptionalException;
import controllers.CiviFormController;
import controllers.LanguageUtils;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.ApplicantModel;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import services.applicant.ApplicantService;
import services.applicant.ApplicantService.ApplicantProgramData;
import services.applicant.ApplicantService.ApplicationPrograms;
import services.program.ProgramDefinition;
import services.program.ProgramService;

/** Class for showing program view based on program slug. */
public final class ProgramSlugHandler {

  private final HttpExecutionContext classLoaderExecutionContext;
  private final ApplicantService applicantService;
  private final ProfileUtils profileUtils;
  private final ProgramService programService;
  private final LanguageUtils languageUtils;
  private final ApplicantRoutes applicantRoutes;

  @Inject
  public ProgramSlugHandler(
      HttpExecutionContext classLoaderExecutionContext,
      ApplicantService applicantService,
      ProfileUtils profileUtils,
      ProgramService programService,
      LanguageUtils languageUtils,
      ApplicantRoutes applicantRoutes) {
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.applicantService = checkNotNull(applicantService);
    this.profileUtils = checkNotNull(profileUtils);
    this.programService = checkNotNull(programService);
    this.languageUtils = checkNotNull(languageUtils);
    this.applicantRoutes = checkNotNull(applicantRoutes);
  }

  public CompletionStage<Result> showProgram(
      CiviFormController controller, Http.Request request, String programSlug) {
    Optional<CiviFormProfile> profile = profileUtils.currentUserProfile(request);

    if (profile.isEmpty()) {
      return CompletableFuture.completedFuture(createGuestSessionAndRedirect(request));
    }

    return profile
        .get()
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

              return getProgramVersionForApplicant(applicantId, programSlug, request)
                  .thenComposeAsync(
                      (Optional<ProgramDefinition> programForExistingApplication) -> {
                        // Check to see if the applicant already has an application
                        // for this program, redirect to program version associated
                        // with that application if so.
                        if (programForExistingApplication.isPresent()) {
                          long programId = programForExistingApplication.get().id();
                          return CompletableFuture.completedFuture(
                              redirectToReviewPage(
                                  controller,
                                  programId,
                                  applicantId,
                                  programSlug,
                                  request,
                                  profile.orElseThrow(
                                      () -> new MissingOptionalException(CiviFormProfile.class))));
                        } else {
                          return programService
                              .getActiveFullProgramDefinitionAsync(programSlug)
                              .thenApply(
                                  activeProgramDefinition ->
                                      redirectToReviewPage(
                                          controller,
                                          activeProgramDefinition.id(),
                                          applicantId,
                                          programSlug,
                                          request,
                                          profile.orElseThrow(
                                              () ->
                                                  new MissingOptionalException(
                                                      CiviFormProfile.class))))
                              .exceptionally(
                                  ex ->
                                      controller
                                          .notFound(ex.getMessage())
                                          .removingFromSession(request, REDIRECT_TO_SESSION_KEY));
                        }
                      },
                      classLoaderExecutionContext.current());
            },
            classLoaderExecutionContext.current());
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
            classLoaderExecutionContext.current());
  }
}
