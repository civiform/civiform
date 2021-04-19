package controllers.applicant;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.ProfileUtils;
import controllers.CiviFormController;

import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;

import forms.ApplicantInformationForm;
import models.Applicant;
import play.data.Form;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import repository.ApplicantRepository;
import services.applicant.ApplicantData;
import views.applicant.ApplicantInformationView;

public final class ApplicantInformationController extends CiviFormController {

  private final HttpExecutionContext httpExecutionContext;
  private final ApplicantInformationView informationView;
  private final ApplicantRepository repository;
  private final FormFactory formFactory;
  private final ProfileUtils profileUtils;

  @Inject
  public ApplicantInformationController(
          HttpExecutionContext httpExecutionContext,
      ApplicantInformationView informationView,
      ApplicantRepository repository,
      FormFactory formFactory,
      ProfileUtils profileUtils) {
    this.httpExecutionContext = httpExecutionContext;
    this.informationView = informationView;
    this.repository = repository;
    this.formFactory = formFactory;
    this.profileUtils = profileUtils;
  }

  public CompletionStage<Result> edit(Http.Request request, long applicantId) {
    return checkApplicantAuthorization(profileUtils, request, applicantId)
        .thenApplyAsync(v -> ok(informationView.render(request, applicantId)), httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                if (ex.getCause() instanceof SecurityException) {
                  return unauthorized();
                }
              }
              throw new RuntimeException(ex);
            });
  }

  public CompletionStage<Result> update(Http.Request request, long applicantId) {
    return checkApplicantAuthorization(profileUtils, request, applicantId)
        .thenApplyAsync(v -> repository.lookupApplicant(applicantId), httpExecutionContext.current())
        .thenApplyAsync(maybeApplicant -> {
          // Set preferred locale.
          Optional<Applicant> applicant = maybeApplicant.toCompletableFuture().join();
          Form<ApplicantInformationForm> form = formFactory.form(ApplicantInformationForm.class);
          ApplicantInformationForm infoForm = form.bindFromRequest(request).get();

          if (applicant.isPresent()) {
            ApplicantData data = applicant.get().getApplicantData();
            data.setPreferredLocale(infoForm.getLocale());
            return repository.updateApplicant(applicant.get());
          } else {
            throw new RuntimeException("Current applicant does not exist");
          }
        }, httpExecutionContext.current())
        .thenComposeAsync(
            v -> supplyAsync(
                () -> redirect(routes.ApplicantProgramsController.index(applicantId))), httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                if (ex.getCause() instanceof SecurityException) {
                  return unauthorized();
                }
              }
              throw new RuntimeException(ex);
            });
  }
}
