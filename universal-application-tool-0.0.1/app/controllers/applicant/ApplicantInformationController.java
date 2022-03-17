package controllers.applicant;

import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableSet;
import controllers.CiviFormController;
import forms.ApplicantInformationForm;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Applicant;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.Lang;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Http.Session;
import play.mvc.Result;
import play.mvc.Results;
import repository.UserRepository;
import services.applicant.ApplicantData;
import services.applicant.ApplicantService;
import services.applicant.exception.ApplicantNotFoundException;
import views.applicant.ApplicantInformationView;

/**
 * Provides methods for editing and updating an applicant's information, such as their preferred
 * language.
 */
public final class ApplicantInformationController extends CiviFormController {

  private final HttpExecutionContext httpExecutionContext;
  private final MessagesApi messagesApi;
  private final ApplicantInformationView informationView;
  private final UserRepository repository;
  private final FormFactory formFactory;
  private final ProfileUtils profileUtils;
  private final ApplicantService applicantService;

  @Inject
  public ApplicantInformationController(
      HttpExecutionContext httpExecutionContext,
      MessagesApi messagesApi,
      ApplicantInformationView informationView,
      UserRepository repository,
      FormFactory formFactory,
      ApplicantService applicantService,
      ProfileUtils profileUtils) {
    this.httpExecutionContext = httpExecutionContext;
    this.messagesApi = messagesApi;
    this.informationView = informationView;
    this.repository = repository;
    this.formFactory = formFactory;
    this.profileUtils = profileUtils;
    this.applicantService = applicantService;
  }

  @Secure
  public CompletionStage<Result> edit(Http.Request request, long applicantId) {
    Optional<String> redirectTo =
        request.session().data().containsKey(REDIRECT_TO_SESSION_KEY)
            ? Optional.of(request.session().data().get(REDIRECT_TO_SESSION_KEY))
            : Optional.empty();

    CompletionStage<String> applicantStage = this.applicantService.getName(applicantId);

    return applicantStage
        .thenComposeAsync(v -> checkApplicantAuthorization(profileUtils, request, applicantId))
        .thenApplyAsync(
            // Since this is before we set the applicant's preferred language, use
            // the default language for now.
            v ->
                ok(
                    informationView.render(
                        request,
                        applicantStage.toCompletableFuture().join(),
                        messagesApi.preferred(ImmutableSet.of(Lang.defaultLang())),
                        applicantId,
                        redirectTo)),
            httpExecutionContext.current())
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

  @Secure
  public CompletionStage<Result> update(Http.Request request, long applicantId) {
    Form<ApplicantInformationForm> form = formFactory.form(ApplicantInformationForm.class);

    if (form.hasErrors()) {
      return supplyAsync(Results::badRequest);
    }

    ApplicantInformationForm infoForm = form.bindFromRequest(request).get();
    String redirectLocation;
    Session session;

    if (infoForm.getRedirectLink().isEmpty()) {
      redirectLocation = routes.ApplicantProgramsController.index(applicantId).url();
      session = request.session();
    } else {
      redirectLocation = infoForm.getRedirectLink();
      session = request.session().removing(REDIRECT_TO_SESSION_KEY);
    }

    return checkApplicantAuthorization(profileUtils, request, applicantId)
        .thenComposeAsync(
            v -> repository.lookupApplicantByEmail(applicantId), httpExecutionContext.current())
        .thenComposeAsync(
            maybeApplicant -> {
              // Set preferred locale.
              if (maybeApplicant.isPresent()) {
                Applicant applicant = maybeApplicant.get();
                ApplicantData data = applicant.getApplicantData();
                data.setPreferredLocale(infoForm.getLocale());
                // Update the applicant, then pass the updated applicant to the next stage.
                return repository
                    .updateApplicant(applicant)
                    .thenApplyAsync(v -> applicant, httpExecutionContext.current());
              } else {
                return CompletableFuture.failedFuture(new ApplicantNotFoundException(applicantId));
              }
            },
            httpExecutionContext.current())
        .thenApplyAsync(
            applicant -> {
              Locale preferredLocale = applicant.getApplicantData().preferredLocale();

              return redirect(redirectLocation)
                  .withLang(preferredLocale, messagesApi)
                  .withSession(session);
            },
            httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                if (ex.getCause() instanceof SecurityException) {
                  return unauthorized();
                }

                if (ex.getCause() instanceof ApplicantNotFoundException) {
                  return badRequest(ex.getCause().getMessage());
                }
              }

              throw new RuntimeException(ex);
            });
  }
}
