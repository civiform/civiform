package controllers.applicant;

import static autovalue.shaded.com.google$.common.base.$Preconditions.checkNotNull;

import auth.ProfileUtils;
import auth.UatProfile;
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
import play.mvc.Http;
import play.mvc.Result;
import repository.ProgramRepository;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.program.ProgramNotFoundException;
import views.applicant.ApplicantUpsellCreateAccountView;

public class RedirectController extends CiviFormController {
  private final ApplicantService applicantService;
  private final ProfileUtils profileUtils;
  private final ProgramRepository programRepository;
  private final ApplicantUpsellCreateAccountView upsellView;
  private final MessagesApi messagesApi;

  @Inject
  public RedirectController(
      ApplicantService applicantService,
      ProfileUtils profileUtils,
      ProgramRepository programRepository,
      ApplicantUpsellCreateAccountView upsellView,
      MessagesApi messagesApi) {
    this.applicantService = checkNotNull(applicantService);
    this.profileUtils = checkNotNull(profileUtils);
    this.programRepository = checkNotNull(programRepository);
    this.upsellView = checkNotNull(upsellView);
    this.messagesApi = checkNotNull(messagesApi);
  }

  @Secure
  public CompletableFuture<Result> programByName(Http.Request request, String programName) {
    Optional<UatProfile> profile = profileUtils.currentUserProfile(request);
    if (profile.isEmpty()) {
      return CompletableFuture.completedFuture(
          redirect(routes.CallbackController.callback("GuestClient")));
    }
    CompletableFuture<Applicant> applicant = profile.get().getApplicant();
    CompletableFuture<Program> program = programRepository.getForSlug(programName);
    return CompletableFuture.allOf(applicant, program)
        .thenApply(
            empty -> {
              if (applicant.isCompletedExceptionally()) {
                return notFound();
              } else if (program.isCompletedExceptionally()) {
                return notFound();
              }
              return redirect(
                  controllers.applicant.routes.ApplicantProgramsController.edit(
                      applicant.join().id, program.join().id));
            });
  }

  @Secure
  public CompletableFuture<Result> considerRegister(
      Http.Request request,
      long applicantId,
      long programId,
      long applicationId,
      String redirectTo) {
    Optional<UatProfile> profile = profileUtils.currentUserProfile(request);
    if (profile.isEmpty()) {
      // should definitely never happen.
      return CompletableFuture.completedFuture(
          badRequest("You are not signed in - you cannot perform this action."));
    }

    CompletionStage<ReadOnlyApplicantProgramService> roApplicantProgramServiceCompletionStage =
        applicantService.getReadOnlyApplicantProgramService(applicantId, programId);
    return checkApplicantAuthorization(profileUtils, request, applicantId)
        .thenComposeAsync(v -> profile.get().getAccount())
        .thenCombineAsync(
            roApplicantProgramServiceCompletionStage,
            (account, roApplicantProgramService) ->
                ok(
                    upsellView.render(
                        request,
                        redirectTo,
                        account,
                        roApplicantProgramService.getProgramTitle(),
                        applicationId,
                        messagesApi.preferred(request),
                        request.flash().get("banner"))))
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
