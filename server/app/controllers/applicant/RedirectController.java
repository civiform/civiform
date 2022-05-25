package controllers.applicant;

import static autovalue.shaded.com.google$.common.base.$Preconditions.checkNotNull;
import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import controllers.CiviFormController;
import controllers.routes;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Applicant;
import models.Program;
import org.pac4j.play.java.Secure;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import repository.ProgramRepository;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.program.ProgramNotFoundException;
import views.applicant.ApplicantUpsellCreateAccountView;

/**
 * Controller for handling methods for deep links. Applicants will be asked to sign-in before they
 * can access the page.
 */
public class RedirectController extends CiviFormController {

  private final HttpExecutionContext httpContext;
  private final ApplicantService applicantService;
  private final ProfileUtils profileUtils;
  private final ProgramRepository programRepository;
  private final ApplicantUpsellCreateAccountView upsellView;
  private final MessagesApi messagesApi;

  @Inject
  public RedirectController(
      HttpExecutionContext httpContext,
      ApplicantService applicantService,
      ProfileUtils profileUtils,
      ProgramRepository programRepository,
      ApplicantUpsellCreateAccountView upsellView,
      MessagesApi messagesApi) {
    this.httpContext = checkNotNull(httpContext);
    this.applicantService = checkNotNull(applicantService);
    this.profileUtils = checkNotNull(profileUtils);
    this.programRepository = checkNotNull(programRepository);
    this.upsellView = checkNotNull(upsellView);
    this.messagesApi = checkNotNull(messagesApi);
  }

  public CompletionStage<Result> programByName(Http.Request request, String programName) {
    Optional<CiviFormProfile> profile = profileUtils.currentUserProfile(request);

    if (profile.isEmpty()) {
      Result result = redirect(routes.HomeController.loginForm(Optional.of("login")));
      result = result.withSession(ImmutableMap.of(REDIRECT_TO_SESSION_KEY, request.uri()));

      return CompletableFuture.completedFuture(result);
    }

    CompletableFuture<Applicant> applicantFuture = profile.get().getApplicant();
    CompletableFuture<Program> programFuture = programRepository.getForSlug(programName);

    return CompletableFuture.allOf(applicantFuture, programFuture)
        .thenApplyAsync(
            empty -> {
              if (applicantFuture.isCompletedExceptionally()) {
                return notFound();
              } else if (programFuture.isCompletedExceptionally()) {
                return notFound();
              }

              Applicant applicant = applicantFuture.join();

              if (!applicant.getApplicantData().hasPreferredLocale()) {
                return redirect(
                        controllers.applicant.routes.ApplicantInformationController.edit(
                            applicant.id))
                    .withSession(request.session().adding(REDIRECT_TO_SESSION_KEY, request.uri()));
              }

              return redirect(
                  controllers.applicant.routes.ApplicantProgramReviewController.preview(
                      applicant.id, programFuture.join().id));
            },
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
