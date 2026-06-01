package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import actions.ProgramDisabledAction;
import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import auth.controllers.MissingOptionalException;
import controllers.CiviFormController;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.pac4j.play.java.Secure;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.With;
import repository.VersionRepository;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.monitoring.MonitoringMetricCounters;
import services.program.BlockDefinition;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import views.applicant.ineligible.ApplicantIneligibleView;

/** Controller for */
@With(ProgramDisabledAction.class)
public class ApplicantAddressCorrectionController extends CiviFormController {

  private final ApplicantService applicantService;
  private final MessagesApi messagesApi;
  private final ApplicantIneligibleView applicantIneligibleView;
  private final SettingsManifest settingsManifest;
  private final ProgramService programService;
  private final ProgramSlugHandler programSlugHandler;
  private final MonitoringMetricCounters metricCounters;

  @Inject
  public ApplicantAddressCorrectionController(
      ApplicantService applicantService,
      MessagesApi messagesApi,
      ApplicantIneligibleView applicantIneligibleView,
      ProfileUtils profileUtils,
      SettingsManifest settingsManifest,
      ProgramService programService,
      VersionRepository versionRepository,
      ProgramSlugHandler programSlugHandler,
      MonitoringMetricCounters metricCounters) {
    super(profileUtils, versionRepository);
    this.applicantService = checkNotNull(applicantService);
    this.messagesApi = checkNotNull(messagesApi);
    this.applicantIneligibleView = checkNotNull(applicantIneligibleView);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.programService = checkNotNull(programService);
    this.programSlugHandler = checkNotNull(programSlugHandler);
    this.metricCounters = checkNotNull(metricCounters);
  }

  /** Renders the application ineligible page for applicants. */
  @Secure(authorizers = Authorizers.Labels.APPLICANT)
  public CompletionStage<Result> ineligible(
      Request request, String programParam, Optional<String> blockId) {
    // Redirect home when the program param is the program id (numeric) but it should be the program
    // slug because the program slug URL is enabled
    boolean programSlugUrlsEnabled = settingsManifest.getProgramSlugUrlsEnabled(request);
    if (programSlugUrlsEnabled && StringUtils.isNumeric(programParam)) {
      metricCounters
          .getUrlWithProgramIdCall()
          .labels("/programs/:programParam/blocks/:blockId/ineligible", programParam)
          .inc();
      return CompletableFuture.completedFuture(redirectToHome());
    }

    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return CompletableFuture.completedFuture(redirectToHome());
    }

    return ineligibleInternal(request, applicantId.get(), programParam, blockId);
  }

  /**
   * Renders the application ineligible page for TIs applying on behalf of clients and CiviForm
   * admins previewing programs.
   */
  @Secure(authorizers = Authorizers.Labels.TI_OR_CIVIFORM_ADMIN)
  public CompletionStage<Result> ineligibleWithApplicantId(
      Request request, long applicantId, String programParam, Optional<String> blockId) {
    // Redirect home when the program param is the program id (numeric) but it should be the program
    // slug because the program slug URL is enabled
    boolean programSlugUrlsEnabled = settingsManifest.getProgramSlugUrlsEnabled(request);
    if (programSlugUrlsEnabled && StringUtils.isNumeric(programParam)) {
      metricCounters
          .getUrlWithProgramIdCall()
          .labels(
              "/applicants/:applicantId/programs/:programParam/blocks/:blockId/ineligible",
              programParam)
          .inc();
      return CompletableFuture.completedFuture(redirectToHome());
    }
    return ineligibleInternal(request, applicantId, programParam, blockId);
  }

  private CompletionStage<Result> ineligibleInternal(
      Request request, long applicantId, String programParam, Optional<String> blockId) {
    boolean programSlugUrlsEnabled = settingsManifest.getProgramSlugUrlsEnabled(request);

    return programSlugHandler
        .resolveProgramParam(programParam, applicantId, programSlugUrlsEnabled)
        .thenCompose(
            programId -> {
              return supplyAsync(
                      () -> {
                        ReadOnlyApplicantProgramService roApplicantProgramService =
                            applicantService
                                .getReadOnlyApplicantProgramService(applicantId, programId)
                                .toCompletableFuture()
                                .join();
                        CiviFormProfile profile = profileUtils.currentUserProfile(request);
                        ApplicantPersonalInfo personalInfo =
                            applicantService
                                .getPersonalInfo(applicantId)
                                .toCompletableFuture()
                                .join();

                        ProgramDefinition programDefinition;
                        Optional<BlockDefinition> blockDefinition = Optional.empty();
                        try {
                          programDefinition = programService.getFullProgramDefinition(programId);
                          if (blockId.isPresent()) {
                            blockDefinition =
                                Optional.of(programDefinition.getBlockDefinition(blockId.get()));
                          }
                        } catch (ProgramNotFoundException
                            | ProgramBlockDefinitionNotFoundException
                            | NoSuchElementException e) {
                          return notFound();
                        }
                        ApplicantIneligibleView.Params params =
                            ApplicantIneligibleView.Params.builder()
                                .setRequest(request)
                                .setApplicantId(applicantId)
                                .setProfile(profile)
                                .setApplicantPersonalInfo(personalInfo)
                                .setProgramDefinition(programDefinition)
                                .setBlockDefinition(blockDefinition)
                                .setRoApplicantProgramService(roApplicantProgramService)
                                .setMessages(messagesApi.preferred(request))
                                .build();
                        return ok(applicantIneligibleView.render(params)).as(Http.MimeTypes.HTML);
                      })
                  .exceptionally(
                      ex -> {
                        Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
                        if (cause instanceof ProgramNotFoundException) {
                          return notFound();
                        }
                        if (cause instanceof MissingOptionalException) {
                          return unauthorized();
                        }
                        throw new RuntimeException(cause);
                      });
            });
  }
}
