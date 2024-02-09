package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import auth.controllers.MissingOptionalException;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.AccountModel;
import models.ApplicationModel;
import org.pac4j.play.java.Secure;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.applicant.ApplicantService.ApplicantProgramData;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applications.ApplicationService;
import services.applications.PdfExporterService;
import services.export.PdfExporter;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import views.applicant.ApplicantCommonIntakeUpsellCreateAccountView;
import views.applicant.ApplicantUpsellCreateAccountView;
import views.components.ToastMessage;

/** Controller for handling methods for upselling applicants. */
public final class UpsellController extends CiviFormController {

  private final HttpExecutionContext httpContext;
  private final ApplicantService applicantService;
  private final ApplicationService applicationService;
  private final ProgramService programService;
  private final ApplicantUpsellCreateAccountView upsellView;
  private final ApplicantCommonIntakeUpsellCreateAccountView cifUpsellView;
  private final MessagesApi messagesApi;
  private final PdfExporterService pdfExporterService;
  private final SettingsManifest settingsManifest;
  private final ApplicantRoutes applicantRoutes;

  @Inject
  public UpsellController(
      HttpExecutionContext httpContext,
      ApplicantService applicantService,
      ApplicationService applicationService,
      ProfileUtils profileUtils,
      ProgramService programService,
      ApplicantUpsellCreateAccountView upsellView,
      ApplicantCommonIntakeUpsellCreateAccountView cifUpsellView,
      MessagesApi messagesApi,
      PdfExporterService pdfExporterService,
      SettingsManifest settingsManifest,
      VersionRepository versionRepository,
      ApplicantRoutes applicantRoutes) {
    super(profileUtils, versionRepository);
    this.httpContext = checkNotNull(httpContext);
    this.applicantService = checkNotNull(applicantService);
    this.applicationService = checkNotNull(applicationService);
    this.programService = checkNotNull(programService);
    this.upsellView = checkNotNull(upsellView);
    this.cifUpsellView = checkNotNull(cifUpsellView);
    this.messagesApi = checkNotNull(messagesApi);
    this.pdfExporterService = checkNotNull(pdfExporterService);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.applicantRoutes = checkNotNull(applicantRoutes);
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

    CompletableFuture<AccountModel> account =
        applicantPersonalInfo
            .thenComposeAsync(
                v -> checkApplicantAuthorization(request, applicantId), httpContext.current())
            .thenComposeAsync(v -> profile.get().getAccount(), httpContext.current())
            .toCompletableFuture();

    CompletableFuture<ReadOnlyApplicantProgramService> roApplicantProgramService =
        applicantService
            .getReadOnlyApplicantProgramService(applicantId, programId)
            .toCompletableFuture();

    CompletableFuture<ApplicantService.ApplicationPrograms> relevantProgramsFuture =
        applicantService
            .relevantProgramsForApplicant(applicantId, profile.get())
            .toCompletableFuture();

    return CompletableFuture.allOf(
            isCommonIntake, account, roApplicantProgramService, relevantProgramsFuture)
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
                        profile.orElseThrow(
                            () -> new MissingOptionalException(CiviFormProfile.class)),
                        maybeEligiblePrograms.orElseGet(ImmutableList::of),
                        messagesApi.preferred(request),
                        toastMessage,
                        applicantRoutes));
              }
              return ok(
                  upsellView.render(
                      request,
                      relevantProgramsFuture.join(),
                      redirectTo,
                      account.join(),
                      roApplicantProgramService.join().getApplicantData().preferredLocale(),
                      roApplicantProgramService.join().getProgramTitle(),
                      roApplicantProgramService.join().getCustomConfirmationMessage(),
                      applicantPersonalInfo.join(),
                      applicantId,
                      profile.orElseThrow(
                          () -> new MissingOptionalException(CiviFormProfile.class)),
                      applicationId,
                      messagesApi.preferred(request),
                      toastMessage,
                      applicantRoutes));
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

  /** Download a PDF file of the application to the program. */
  @Secure
  public CompletionStage<Result> download(
      Http.Request request, long applicationId, long applicantId) throws ProgramNotFoundException {
    if (!settingsManifest.getApplicationExportable(request)) {
      return CompletableFuture.completedFuture(forbidden());
    }
    CompletableFuture<Void> authorization = checkApplicantAuthorization(request, applicantId);
    CompletableFuture<Optional<ApplicationModel>> applicationMaybe =
        applicationService.getApplicationAsync(applicationId).toCompletableFuture();

    return CompletableFuture.allOf(applicationMaybe, authorization)
        .thenApplyAsync(
            check -> {
              PdfExporter.InMemoryPdf pdf =
                  pdfExporterService.generatePdf(
                      applicationMaybe.join().get(),
                      /* showEligibilityText= */ false,
                      /* includeHiddenBlocks= */ false);

              return ok(pdf.getByteArray())
                  .as("application/pdf")
                  .withHeader(
                      "Content-Disposition",
                      String.format("attachment; filename=\"%s\"", pdf.getFileName()));
            })
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof SecurityException) {
                  return unauthorized(cause.toString());
                }
                if (cause instanceof ProgramNotFoundException) {
                  return notFound(cause.toString());
                }
                if (cause instanceof NoSuchElementException) {
                  return notFound(cause.toString());
                }
              }
              throw new RuntimeException(ex);
            });
  }
}
