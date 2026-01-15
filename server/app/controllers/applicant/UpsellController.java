package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
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
import views.applicant.ApplicantPreScreenerUpsellView;
import views.applicant.ApplicantUpsellView;
import views.applicant.UpsellParams;

/** Controller for handling methods for upselling applicants. */
public final class UpsellController extends CiviFormController {

  private final ClassLoaderExecutionContext classLoaderExecutionContext;
  private final ApplicantService applicantService;
  private final ApplicationService applicationService;
  private final ProgramService programService;
  private final ApplicantUpsellView upsellView;
  private final ApplicantPreScreenerUpsellView preScreenerUpsellView;
  private final MessagesApi messagesApi;
  private final PdfExporterService pdfExporterService;

  @Inject
  public UpsellController(
      ClassLoaderExecutionContext classLoaderExecutionContext,
      ApplicantService applicantService,
      ApplicationService applicationService,
      ProfileUtils profileUtils,
      ProgramService programService,
      ApplicantUpsellView applicantUpsellView,
      ApplicantPreScreenerUpsellView applicantPreScreenerUpsellView,
      MessagesApi messagesApi,
      PdfExporterService pdfExporterService,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.applicantService = checkNotNull(applicantService);
    this.applicationService = checkNotNull(applicationService);
    this.programService = checkNotNull(programService);
    this.upsellView = checkNotNull(applicantUpsellView);
    this.preScreenerUpsellView = checkNotNull(applicantPreScreenerUpsellView);
    this.messagesApi = checkNotNull(messagesApi);
    this.pdfExporterService = checkNotNull(pdfExporterService);
  }

  @Secure
  public CompletionStage<Result> considerRegister(
      Http.Request request,
      long applicantId,
      long programId,
      long applicationId,
      String redirectTo,
      String submitTime) {
    CiviFormProfile profile = profileUtils.currentUserProfile(request);

    CompletableFuture<Boolean> isPreScreener =
        programService
            .getFullProgramDefinitionAsync(programId)
            .thenApplyAsync(ProgramDefinition::isPreScreenerForm)
            .toCompletableFuture();

    CompletableFuture<ApplicantPersonalInfo> applicantPersonalInfo =
        applicantService.getPersonalInfo(applicantId).toCompletableFuture();

    CompletableFuture<AccountModel> account =
        applicantPersonalInfo
            .thenComposeAsync(
                v -> checkApplicantAuthorization(request, applicantId),
                classLoaderExecutionContext.current())
            .thenComposeAsync(v -> profile.getAccount(), classLoaderExecutionContext.current())
            .toCompletableFuture();

    CompletableFuture<ReadOnlyApplicantProgramService> roApplicantProgramService =
        applicantService
            .getReadOnlyApplicantProgramService(applicantId, programId)
            .toCompletableFuture();

    CompletableFuture<ApplicantService.ApplicationPrograms> relevantProgramsFuture =
        applicantService
            .relevantProgramsForApplicant(applicantId, profile, request)
            .toCompletableFuture();

    return CompletableFuture.allOf(
        isPreScreener, account, roApplicantProgramService, relevantProgramsFuture)
        .thenComposeAsync(
            ignored -> {
              if (!isPreScreener.join()) {
                // Only the pre-screener form needs to get the applicant's eligible
                // programs this way.
                Optional<ImmutableList<ApplicantProgramData>> result = Optional.empty();
                return CompletableFuture.completedFuture(result);
              }

              return applicantPersonalInfo
                  .thenComposeAsync(v -> checkApplicantAuthorization(request, applicantId))
                  .thenComposeAsync(
                      // We are already checking if profile is empty
                      v ->
                          applicantService.maybeEligibleProgramsForApplicant(
                              applicantId, profile, request),
                      classLoaderExecutionContext.current())
                  .thenApplyAsync(Optional::of);
            })
        .thenApplyAsync(
            maybeEligiblePrograms -> {
              Optional<String> toastMessageValue = request.flash().get(FlashKey.BANNER);

              Instant instant = Instant.parse(submitTime);
              Date submitDate = Date.from(instant);
              DateFormat dateFormat =
                  DateFormat.getDateInstance(
                      DateFormat.LONG,
                      roApplicantProgramService.join().getApplicantData().preferredLocale());
              String formattedDate = dateFormat.format(submitDate);

              UpsellParams.Builder paramsBuilder =
                  UpsellParams.builder()
                      .setRequest(request)
                      .setProgramTitle(roApplicantProgramService.join().getProgramTitle())
                      .setProgramShortDescription(
                          roApplicantProgramService.join().getProgramShortDescription())
                      .setProfile(profile)
                      .setApplicantPersonalInfo(applicantPersonalInfo.join())
                      .setApplicationId(applicationId)
                      .setMessages(messagesApi.preferred(request))
                      .setBannerMessage(toastMessageValue)
                      .setCompletedProgramId(programId)
                      .setCustomConfirmationMessage(
                          roApplicantProgramService.join().getCustomConfirmationMessage())
                      .setApplicantId(applicantId)
                      .setDateSubmitted(formattedDate);

              if (isPreScreener.join()) {
                UpsellParams upsellParams =
                    paramsBuilder
                        .setEligiblePrograms(maybeEligiblePrograms.orElseGet(ImmutableList::of))
                        .build();
                return ok(preScreenerUpsellView.render(upsellParams)).as(Http.MimeTypes.HTML);
              } else {
                UpsellParams upsellParams =
                    paramsBuilder
                        .setEligiblePrograms(
                            relevantProgramsFuture.join().unappliedAndPotentiallyEligible())
                        .build();
                return ok(upsellView.render(upsellParams)).as(Http.MimeTypes.HTML);
              }
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
