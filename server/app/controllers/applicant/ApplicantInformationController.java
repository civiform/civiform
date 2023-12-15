package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import auth.controllers.MissingOptionalException;
import controllers.CiviFormController;
import forms.ApplicantInformationForm;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.ApplicantModel;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Http.Session;
import play.mvc.Result;
import play.mvc.Results;
import repository.AccountRepository;
import repository.VersionRepository;
import services.applicant.ApplicantData;
import services.applicant.exception.ApplicantNotFoundException;
import views.applicant.ApplicantLayout;

/**
 * Provides methods for editing and updating an applicant's information, such as their preferred
 * language.
 */
public final class ApplicantInformationController extends CiviFormController {

  private final HttpExecutionContext httpExecutionContext;
  private final MessagesApi messagesApi;
  private final AccountRepository repository;
  private final FormFactory formFactory;
  private final ApplicantLayout layout;
  private final ApplicantRoutes applicantRoutes;

  @Inject
  public ApplicantInformationController(
      HttpExecutionContext httpExecutionContext,
      MessagesApi messagesApi,
      AccountRepository repository,
      FormFactory formFactory,
      ProfileUtils profileUtils,
      ApplicantLayout layout,
      VersionRepository versionRepository,
      ApplicantRoutes applicantRoutes) {
    super(profileUtils, versionRepository);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.messagesApi = checkNotNull(messagesApi);
    this.repository = checkNotNull(repository);
    this.formFactory = checkNotNull(formFactory);
    this.layout = checkNotNull(layout);
    this.applicantRoutes = checkNotNull(applicantRoutes);
  }

  /**
   * Sets the applicant's preferred language based on their browser session, then redirects them
   * accordingly.
   */
  @Secure
  public CompletionStage<Result> setLangFromBrowser(Http.Request request, long applicantId) {

    return checkApplicantAuthorization(request, applicantId)
        .thenComposeAsync(
            v -> repository.lookupApplicant(applicantId), httpExecutionContext.current())
        .thenComposeAsync(
            maybeApplicant ->
                updateApplicantPreferredLanguage(
                    maybeApplicant,
                    applicantId,
                    Locale.forLanguageTag(
                        layout.languageSelector.getPreferredLangage(request).code())),
            httpExecutionContext.current())
        .thenApplyAsync(
            applicant -> {
              Locale preferredLocale = applicant.getApplicantData().preferredLocale();

              String redirectLink;
              if (request.session().data().containsKey(REDIRECT_TO_SESSION_KEY)) {
                redirectLink = request.session().data().get(REDIRECT_TO_SESSION_KEY);
              } else if (profileUtils.currentUserProfile(request).get().isTrustedIntermediary()) {
                redirectLink =
                    controllers.ti.routes.TrustedIntermediaryController.dashboard(
                            /* nameQuery= */ Optional.empty(),
                            /* dateQuery= */ Optional.empty(),
                            /* page= */ Optional.of(1))
                        .url();
              } else {
                CiviFormProfile profile =
                    profileUtils
                        .currentUserProfile(request)
                        .orElseThrow(() -> new MissingOptionalException(CiviFormProfile.class));
                redirectLink = applicantRoutes.index(profile, applicantId).url();
              }

              return redirect(redirectLink)
                  .withLang(preferredLocale, messagesApi)
                  .withSession(request.session());
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

  /**
   * Sets the applicant's preferred language based on their language form selection, then redirects
   * them.
   */
  @Secure
  public CompletionStage<Result> setLangFromSwitcher(Http.Request request, long applicantId) {
    Form<ApplicantInformationForm> form = formFactory.form(ApplicantInformationForm.class);

    if (form.hasErrors()) {
      return supplyAsync(Results::badRequest);
    }

    ApplicantInformationForm infoForm = form.bindFromRequest(request).get();
    String redirectLocation;
    Session session;
    CiviFormProfile profile =
        profileUtils
            .currentUserProfile(request)
            .orElseThrow(() -> new MissingOptionalException(CiviFormProfile.class));

    if (infoForm.getRedirectLink().isEmpty()) {
      redirectLocation = applicantRoutes.index(profile, applicantId).url();
      session = request.session();
    } else {
      redirectLocation = infoForm.getRedirectLink();
      session = request.session().removing(REDIRECT_TO_SESSION_KEY);
    }

    return checkApplicantAuthorization(request, applicantId)
        .thenComposeAsync(
            v -> repository.lookupApplicant(applicantId), httpExecutionContext.current())
        .thenComposeAsync(
            maybeApplicant ->
                updateApplicantPreferredLanguage(maybeApplicant, applicantId, infoForm.getLocale()),
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

  private CompletionStage<ApplicantModel> updateApplicantPreferredLanguage(
      Optional<ApplicantModel> maybeApplicant, long applicantId, Locale locale) {
    // Set preferred locale.
    if (maybeApplicant.isPresent()) {
      ApplicantModel applicant = maybeApplicant.get();
      ApplicantData data = applicant.getApplicantData();
      data.setPreferredLocale(locale);
      // Update the applicant, then pass the updated applicant to the next stage.
      return repository
          .updateApplicant(applicant)
          .thenApplyAsync(v -> applicant, httpExecutionContext.current());
    } else {
      return CompletableFuture.failedFuture(new ApplicantNotFoundException(applicantId));
    }
  }
}
