package controllers.applicant;

import auth.ProfileUtils;
import auth.UatProfile;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import controllers.CiviFormController;
import controllers.routes;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import models.Applicant;
import models.Program;
import org.pac4j.play.java.Secure;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.mvc.Result;
import repository.ProgramRepository;
import views.applicant.ApplicantUpsellCreateAccountView;

public class RedirectController extends CiviFormController {
  private final ProfileUtils profileUtils;
  private final ProgramRepository programRepository;
  private final ApplicantUpsellCreateAccountView upsellView;
  private final MessagesApi messagesApi;

  @Inject
  public RedirectController(
      ProfileUtils profileUtils,
      ProgramRepository programRepository,
      ApplicantUpsellCreateAccountView upsellView,
      MessagesApi messagesApi) {
    this.profileUtils = Preconditions.checkNotNull(profileUtils);
    this.programRepository = Preconditions.checkNotNull(programRepository);
    this.upsellView = Preconditions.checkNotNull(upsellView);
    this.messagesApi = Preconditions.checkNotNull(messagesApi);
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
  public CompletableFuture<Result> considerRegister(Http.Request request, String redirectTo) {
    Optional<UatProfile> profile = profileUtils.currentUserProfile(request);
    if (profile.isEmpty()) {
      // should definitely never happen.
      return CompletableFuture.completedFuture(
          badRequest("You are not signed in - you cannot perform this action."));
    }
    return profile
        .get()
        .getAccount()
        .thenApplyAsync(
            account -> {
              // Don't show this page to TIs, or anyone with an email address already.
              if (!Strings.isNullOrEmpty(account.getEmailAddress())) {
                return redirect(redirectTo);
              } else if (account.getMemberOfGroup().isPresent()) {
                return redirect(redirectTo);
              }

              return ok(
                  upsellView.render(
                      request,
                      redirectTo,
                      messagesApi.preferred(request),
                      account.getApplicantName()));
            });
  }
}
