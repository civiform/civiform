package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import auth.controllers.MissingOptionalException;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import controllers.FlashKey;
import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;
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
import play.libs.concurrent.ClassLoaderExecutionContext;
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
import views.applicant.NorthStarApplicantCommonIntakeUpsellView;
import views.applicant.NorthStarApplicantUpsellView;
import views.applicant.UpsellParams;
import views.components.ToastMessage;

/** Controller for handling methods for upselling applicants. */
public final class UpsellController extends CiviFormController {

  private final ClassLoaderExecutionContext classLoaderExecutionContext;
  private final ApplicantService applicantService;
  private final ApplicationService applicationService;
  private final ProgramService programService;
  private final ApplicantUpsellCreateAccountView upsellView;
  private final ApplicantCommonIntakeUpsellCreateAccountView cifUpsellView;
  private final NorthStarApplicantUpsellView northStarUpsellView;
  private final NorthStarApplicantCommonIntakeUpsellView northStarCommonIntakeUpsellView;
  private final MessagesApi messagesApi;
  private final PdfExporterService pdfExporterService;
  private final SettingsManifest settingsManifest;
  private final ApplicantRoutes applicantRoutes;

  @Inject
  public UpsellController(
      ClassLoaderExecutionContext classLoaderExecutionContext,
      ApplicantService applicantService,
      ApplicationService applicationService,
      ProfileUtils profileUtils,
      ProgramService programService,
      ApplicantUpsellCreateAccountView upsellView,
      ApplicantCommonIntakeUpsellCreateAccountView cifUpsellView,
      NorthStarApplicantUpsellView northStarApplicantUpsellView,
      NorthStarApplicantCommonIntakeUpsellView northStarApplicantCommonIntakeUpsellView,
      MessagesApi messagesApi,
      PdfExporterService pdfExporterService,
      SettingsManifest settingsManifest,
      VersionRepository versionRepository,
      ApplicantRoutes applicantRoutes) {
    super(profileUtils, versionRepository);
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.applicantService = checkNotNull(applicantService);
    this.applicationService = checkNotNull(applicationService);
    this.programService = checkNotNull(programService);
    this.upsellView = checkNotNull(upsellView);
    this.cifUpsellView = checkNotNull(cifUpsellView);
    this.northStarUpsellView = checkNotNull(northStarApplicantUpsellView);
    this.northStarCommonIntakeUpsellView = checkNotNull(northStarApplicantCommonIntakeUpsellView);
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
      String redirectTo,
      String submitTime) {
    Optional<CiviFormProfile> profile = profileUtils.currentUserProfile(request);
    if (profile.isEmpty()) {
      // should definitely never happen.
      return CompletableFuture.completedFuture(
          badRequest("You are not signed in - you cannot perform this action."));
    }

    CompletableFuture<Boolean> isCommonIntake =
        programService
            .getFullProgramDefinitionAsync(programId)
            .thenApplyAsync(ProgramDefinition::isCommonIntakeForm)
            .toCompletableFuture();

    CompletableFuture<ApplicantPersonalInfo> applicantPersonalInfo =
        applicantService.getPersonalInfo(applicantId).toCompletableFuture();

    CompletableFuture<AccountModel> account =
        applicantPersonalInfo
            .thenComposeAsync(
                v -> checkApplicantAuthorization(request, applicantId),
                classLoaderExecutionContext.current())
            .thenComposeAsync(
                v -> profile.get().getAccount(), classLoaderExecutionContext.current())
            .toCompletableFuture();

    CompletableFuture<ReadOnlyApplicantProgramService> roApplicantProgramService =
        applicantService
            .getReadOnlyApplicantProgramService(applicantId, programId)
            .toCompletableFuture();

    CompletableFuture<ApplicantService.ApplicationPrograms> relevantProgramsFuture =
        applicantService
            .relevantProgramsForApplicant(applicantId, profile.get(), request)
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
                              applicantId, profile.get(), request),
                      classLoaderExecutionContext.current())
                  .thenApplyAsync(Optional::of);
            })
        .thenApplyAsync(
            maybeEligiblePrograms -> {
              Optional<String> toastMessageValue = request.flash().get(FlashKey.BANNER);
              Optional<ToastMessage> toastMessage =
                  toastMessageValue.map(m -> ToastMessage.alert(m));

              Instant instant = Instant.parse(submitTime);
              Date submitDate = Date.from(instant);
              DateFormat dateFormat =
                  DateFormat.getDateInstance(
                      DateFormat.LONG,
                      roApplicantProgramService.join().getApplicantData().preferredLocale());
              String formattedDate = dateFormat.format(submitDate);

              if (settingsManifest.getNorthStarApplicantUi(request)) {
                UpsellParams.Builder paramsBuilder =
                    UpsellParams.builder()
                        .setRequest(request)
                        .setProfile(
                            profile.orElseThrow(
                                () -> new MissingOptionalException(CiviFormProfile.class)))
                        .setApplicantPersonalInfo(applicantPersonalInfo.join())
                        .setApplicationId(applicationId)
                        .setMessages(messagesApi.preferred(request))
                        .setBannerMessage(toastMessageValue)
                        .setCustomConfirmationMessage(
                            roApplicantProgramService.join().getCustomConfirmationMessage())
                        .setApplicantId(applicantId)
                        .setDateSubmitted(formattedDate);

                if (isCommonIntake.join()) {
                  UpsellParams upsellParams =
                      paramsBuilder
                          .setEligiblePrograms(maybeEligiblePrograms.orElseGet(ImmutableList::of))
                          .setDateSubmitted(formattedDate)
                          .build();
                  return ok(northStarCommonIntakeUpsellView.render(upsellParams))
                      .as(Http.MimeTypes.HTML);
                } else {
                  UpsellParams upsellParams =
                      paramsBuilder
                          .setProgramTitle(roApplicantProgramService.join().getProgramTitle())
                          .setProgramDescription(
                              roApplicantProgramService.join().getProgramDescription())
                          .build();
                  return ok(northStarUpsellView.render(upsellParams)).as(Http.MimeTypes.HTML);
                }
              } else if (isCommonIntake.join()) {
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
            classLoaderExecutionContext.current())
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
                  pdfExporterService.generateApplicationPdf(
                      applicationMaybe.join().get(), /* isAdmin= */ false);

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
