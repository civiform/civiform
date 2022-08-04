package controllers.applicant;

import static autovalue.shaded.com.google$.common.base.$Preconditions.checkNotNull;
import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.CiviFormController;
import controllers.LanguageUtils;
import controllers.routes;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Applicant;
import models.LifecycleStage;
import org.pac4j.play.java.Secure;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.applicant.ApplicantUpsellCreateAccountView;

/**
 * Controller for handling methods for deep links. Applicants will be asked to sign-in before they
 * can access the page.
 */
public final class RedirectController extends CiviFormController {

  private final HttpExecutionContext httpContext;
  private final ApplicantService applicantService;
  private final ProfileUtils profileUtils;
  private final ProgramService programService;
  private final ApplicantUpsellCreateAccountView upsellView;
  private final MessagesApi messagesApi;
  private final LanguageUtils languageUtils;

  @Inject
  public RedirectController(
      HttpExecutionContext httpContext,
      ApplicantService applicantService,
      ProfileUtils profileUtils,
      ProgramService programService,
      ApplicantUpsellCreateAccountView upsellView,
      MessagesApi messagesApi,
      LanguageUtils languageUtils) {
    this.httpContext = checkNotNull(httpContext);
    this.applicantService = checkNotNull(applicantService);
    this.profileUtils = checkNotNull(profileUtils);
    this.programService = checkNotNull(programService);
    this.upsellView = checkNotNull(upsellView);
    this.messagesApi = checkNotNull(messagesApi);
    this.languageUtils = checkNotNull(languageUtils);
  }

  public CompletionStage<Result> programBySlug(Http.Request request, String programSlug) {
    Optional<CiviFormProfile> profile = profileUtils.currentUserProfile(request);

    if (profile.isEmpty()) {
      Result result = redirect(routes.HomeController.loginForm(Optional.of("login")));
      result = result.withSession(ImmutableMap.of(REDIRECT_TO_SESSION_KEY, request.uri()));

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
                            controllers.applicant.routes.ApplicantInformationController.edit(
                                applicantId))
                        .withSession(
                            request.session().adding(REDIRECT_TO_SESSION_KEY, request.uri())));
              }

              return getProgramVersionForApplicant(applicantId, programSlug)
                  .thenComposeAsync(
                      (Optional<ProgramDefinition> programForExistingApplication) -> {
                        // Check to see if the applicant already has an application
                        // for this program, redirect to program version associated
                        // with that application if so.
                        if (programForExistingApplication.isPresent()) {
                          return CompletableFuture.completedFuture(
                              redirect(
                                  controllers.applicant.routes.ApplicantProgramReviewController
                                      .preview(
                                          applicantId, programForExistingApplication.get().id())));
                        }

                        return redirectToActiveProgram(applicantId, programSlug);
                      },
                      httpContext.current());
            },
            httpContext.current());
  }

  private CompletionStage<Result> redirectToActiveProgram(long applicantId, String programSlug) {
    return programService
        .getActiveProgramDefinitionAsync(programSlug)
        .thenApplyAsync(
            (activeProgramDefinition) ->
                redirect(
                    controllers.applicant.routes.ApplicantProgramReviewController.preview(
                        applicantId, activeProgramDefinition.id())),
            httpContext.current());
  }

  private CompletionStage<Optional<ProgramDefinition>> getProgramVersionForApplicant(
      long applicantId, String programSlug) {
    return applicantService
        .relevantPrograms(applicantId)
        .thenApplyAsync(
            (ImmutableMap<LifecycleStage, ImmutableList<ProgramDefinition>> relevantPrograms) ->
                relevantPrograms.values().stream()
                    .flatMap(Collection::stream)
                    .filter(program -> program.slug().equals(programSlug))
                    .findAny(),
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

    CompletionStage<Optional<String>> applicantName = applicantService.getName(applicantId);
    CompletionStage<ReadOnlyApplicantProgramService> roApplicantProgramServiceCompletionStage =
        applicantService.getReadOnlyApplicantProgramService(applicantId, programId);
    return applicantName
        .thenComposeAsync(
            v -> checkApplicantAuthorization(profileUtils, request, applicantId),
            httpContext.current())
        .thenComposeAsync(v -> profile.get().getAccount(), httpContext.current())
        .thenCombineAsync(
            roApplicantProgramServiceCompletionStage,
            (account, roApplicantProgramService) ->
                ok(
                    upsellView.render(
                        request,
                        redirectTo,
                        account,
                        roApplicantProgramService.getProgramTitle(),
                        applicantName.toCompletableFuture().join(),
                        applicationId,
                        messagesApi.preferred(request),
                        request.flash().get("banner"))),
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
