package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;

import auth.CiviFormProfile;
import auth.GuestClient;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.CiviFormController;
import controllers.LanguageUtils;
import controllers.routes;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Account;
import models.Applicant;
import org.pac4j.play.java.Secure;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.applicant.ApplicantService.ApplicantProgramData;
import services.applicant.ApplicantService.ApplicationPrograms;
import services.applicant.ReadOnlyApplicantProgramService;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.applicant.ApplicantCommonIntakeUpsellCreateAccountView;
import views.applicant.ApplicantUpsellCreateAccountView;
import views.components.ToastMessage;

/**
 * Controller for handling methods for deep links. Applicants will be asked to sign-in before they
 * can access the page.
 */
public final class RedirectController extends CiviFormController {

  private final HttpExecutionContext httpContext;
  private final ApplicantService applicantService;
  private final ProgramService programService;
  private final ApplicantUpsellCreateAccountView upsellView;
  private final ApplicantCommonIntakeUpsellCreateAccountView cifUpsellView;
  private final MessagesApi messagesApi;
  private final LanguageUtils languageUtils;

  @Inject
  public RedirectController(
      HttpExecutionContext httpContext,
      ApplicantService applicantService,
      ProfileUtils profileUtils,
      ProgramService programService,
      ApplicantUpsellCreateAccountView upsellView,
      ApplicantCommonIntakeUpsellCreateAccountView cifUpsellView,
      MessagesApi messagesApi,
      VersionRepository versionRepository,
      LanguageUtils languageUtils) {
    super(profileUtils, versionRepository);
    this.httpContext = checkNotNull(httpContext);
    this.applicantService = checkNotNull(applicantService);
    this.programService = checkNotNull(programService);
    this.upsellView = checkNotNull(upsellView);
    this.cifUpsellView = checkNotNull(cifUpsellView);
    this.messagesApi = checkNotNull(messagesApi);
    this.languageUtils = checkNotNull(languageUtils);
  }

  public CompletionStage<Result> programBySlug(Http.Request request, String programSlug) {
    Optional<CiviFormProfile> profile = profileUtils.currentUserProfile(request);

    if (profile.isEmpty()) {
      // If there isn't currently a session, create a guest session using the CallbackController,
      // and add a session key that asks it to redirect back here when the profile will be present.
      Result result =
          redirect(routes.CallbackController.callback(GuestClient.CLIENT_NAME).url())
              .withSession(ImmutableMap.of(REDIRECT_TO_SESSION_KEY, request.uri()));

      return CompletableFuture.completedFuture(result);
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

              return getProgramVersionForApplicant(applicantId, programSlug, request)
                  .thenComposeAsync(
                      (Optional<ProgramDefinition> programForExistingApplication) -> {
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

  @Secure
  public CompletionStage<Result> considerRegister(
      Http.Request request,
      long applicantId,
      long programId,
      long applicationId,
      String redirectTo) {
    Optional<CiviFormProfile> profile = profileUtils.currentUserProfile(request);
    if (profile.isEmpty()) {
      // should definitely never happen.
      return CompletableFuture.completedFuture(
          badRequest("You are not signed in - you cannot perform this action."));
    }

    CompletableFuture<Boolean> isCommonIntake =
        programService
            .getProgramDefinitionAsync(programId)
            .thenApplyAsync(ProgramDefinition::isCommonIntakeForm)
            .toCompletableFuture();

    CompletableFuture<ApplicantPersonalInfo> applicantPersonalInfo =
        applicantService.getPersonalInfo(applicantId).toCompletableFuture();

    CompletableFuture<Account> account =
        applicantPersonalInfo
            .thenComposeAsync(
                v -> checkApplicantAuthorization(request, applicantId), httpContext.current())
            .thenComposeAsync(v -> profile.get().getAccount(), httpContext.current())
            .toCompletableFuture();

    CompletableFuture<ReadOnlyApplicantProgramService> roApplicantProgramService =
        applicantService
            .getReadOnlyApplicantProgramService(applicantId, programId)
            .toCompletableFuture();

    return CompletableFuture.allOf(isCommonIntake, account, roApplicantProgramService)
        .thenComposeAsync(
            ignored -> {
              if (!isCommonIntake.join()) {
                // If this isn't the common intake form, we don't need to make the
                // call to get the applicant's eligible programs.
                Optional<ImmutableList<ApplicantProgramData>> result = Optional.empty();
                return CompletableFuture.completedFuture(result);
              }

              return applicantPersonalInfo
                  .thenComposeAsync(v -> checkApplicantAuthorization(request, applicantId))
                  .thenComposeAsync(
                      // we are already checking if profile is empty
                      v ->
                          applicantService.maybeEligibleProgramsForApplicant(
                              applicantId, profile.get()),
                      httpContext.current())
                  .thenApplyAsync(Optional::of);
            })
        .thenApplyAsync(
            maybeEligiblePrograms -> {
              Optional<ToastMessage> toastMessage =
                  request.flash().get("banner").map(m -> ToastMessage.alert(m));

              if (isCommonIntake.join()) {
                return ok(
                    cifUpsellView.render(
                        request,
                        redirectTo,
                        account.join(),
                        applicantPersonalInfo.join(),
                        applicantId,
                        programId,
                        profileUtils
                            .currentUserProfile(request)
                            .orElseThrow()
                            .isTrustedIntermediary(),
                        maybeEligiblePrograms.orElseGet(ImmutableList::of),
                        messagesApi.preferred(request),
                        toastMessage));
              }

              return ok(
                  upsellView.render(
                      request,
                      redirectTo,
                      account.join(),
                      roApplicantProgramService.join().getApplicantData().preferredLocale(),
                      roApplicantProgramService.join().getProgramTitle(),
                      roApplicantProgramService.join().getCustomConfirmationMessage(),
                      applicantPersonalInfo.join(),
                      applicantId,
                      applicationId,
                      messagesApi.preferred(request),
                      toastMessage));
            },
            httpContext.current())
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
              }
              throw new RuntimeException(ex);
            });
  }
}
