package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import actions.ProgramDisabledAction;
import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import auth.controllers.MissingOptionalException;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import controllers.geo.AddressSuggestionJsonSerializer;
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
import services.applicant.Block;
import services.applicant.ReadOnlyApplicantProgramService;
import services.geo.AddressSuggestion;
import services.geo.AddressSuggestionGroup;
import services.monitoring.MonitoringMetricCounters;
import services.program.ProgramNotFoundException;
import services.settings.SettingsManifest;
import views.applicant.addresscorrection.AddressCorrectionBlockView;

/** Controller for */
@With(ProgramDisabledAction.class)
public class ApplicantAddressCorrectionController extends CiviFormController {

  private final ApplicantService applicantService;
  private final MessagesApi messagesApi;
  private final AddressCorrectionBlockView addressCorrectionBlockView;
  private final SettingsManifest settingsManifest;
  private final ProgramSlugHandler programSlugHandler;
  private final AddressSuggestionJsonSerializer addressSuggestionJsonSerializer;
  private final MonitoringMetricCounters metricCounters;

  @Inject
  public ApplicantAddressCorrectionController(
      ApplicantService applicantService,
      MessagesApi messagesApi,
      AddressCorrectionBlockView addressCorrectionBlockView,
      ProfileUtils profileUtils,
      SettingsManifest settingsManifest,
      VersionRepository versionRepository,
      ProgramSlugHandler programSlugHandler,
      AddressSuggestionJsonSerializer addressSuggestionJsonSerializer,
      MonitoringMetricCounters metricCounters) {
    super(profileUtils, versionRepository);
    this.applicantService = checkNotNull(applicantService);
    this.messagesApi = checkNotNull(messagesApi);
    this.addressCorrectionBlockView = checkNotNull(addressCorrectionBlockView);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.programSlugHandler = checkNotNull(programSlugHandler);
    this.addressSuggestionJsonSerializer = checkNotNull(addressSuggestionJsonSerializer);
    this.metricCounters = checkNotNull(metricCounters);
  }

  /** Renders the application address correction page for applicants. */
  @Secure(authorizers = Authorizers.Labels.APPLICANT)
  public CompletionStage<Result> addressCorrection(
      Request request,
      String programParam,
      String blockId,
      boolean inReview,
      ApplicantRequestedActionWrapper applicantRequestedActionWrapper) {
    // Redirect home when the program param is the program id (numeric) but it should be the program
    // slug because the program slug URL is enabled
    boolean programSlugUrlsEnabled = settingsManifest.getProgramSlugUrlsEnabled(request);
    if (programSlugUrlsEnabled && StringUtils.isNumeric(programParam)) {
      metricCounters
          .getUrlWithProgramIdCall()
          .labels("/programs/:programParam/blocks/:blockId/addressCorrection", programParam)
          .inc();
      return CompletableFuture.completedFuture(redirectToHome());
    }

    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return CompletableFuture.completedFuture(redirectToHome());
    }

    return addressCorrectionInternal(
        request,
        applicantId.get(),
        programParam,
        blockId,
        inReview,
        applicantRequestedActionWrapper);
  }

  /**
   * Renders the application ineligible page for TIs applying on behalf of clients and CiviForm
   * admins previewing programs.
   */
  @Secure(authorizers = Authorizers.Labels.TI_OR_CIVIFORM_ADMIN)
  public CompletionStage<Result> addressCorrectionWithApplicantId(
      Request request,
      long applicantId,
      String programParam,
      String blockId,
      boolean inReview,
      ApplicantRequestedActionWrapper applicantRequestedActionWrapper) {
    // Redirect home when the program param is the program id (numeric) but it should be the program
    // slug because the program slug URL is enabled
    boolean programSlugUrlsEnabled = settingsManifest.getProgramSlugUrlsEnabled(request);
    if (programSlugUrlsEnabled && StringUtils.isNumeric(programParam)) {
      metricCounters
          .getUrlWithProgramIdCall()
          .labels(
              "/applicants/:applicantId/programs/:programParam/blocks/:blockId/addressCorrection",
              programParam)
          .inc();
      return CompletableFuture.completedFuture(redirectToHome());
    }
    return addressCorrectionInternal(
        request, applicantId, programParam, blockId, inReview, applicantRequestedActionWrapper);
  }

  private CompletionStage<Result> addressCorrectionInternal(
      Request request,
      long applicantId,
      String programParam,
      String blockId,
      boolean inReview,
      ApplicantRequestedActionWrapper applicantRequestedActionWrapper) {

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
                        Block thisBlockUpdated =
                            roApplicantProgramService.getActiveBlock(blockId).get();

                        boolean isEligibilityEnabledOnThisBlock =
                            thisBlockUpdated.getLeafAddressNodeServiceAreaIds().isPresent();
                        ApplicantPersonalInfo personalInfo =
                            applicantService
                                .getPersonalInfo(applicantId)
                                .toCompletableFuture()
                                .join();

                        AddressSuggestionGroup addressSuggestionGroup =
                            applicantService
                                .getAddressSuggestionGroup(thisBlockUpdated)
                                .toCompletableFuture()
                                .join();
                        ImmutableList<AddressSuggestion> suggestions =
                            addressSuggestionGroup.getAddressSuggestions();
                        String addressSuggestionJson =
                            addressSuggestionJsonSerializer.serialize(suggestions);
                        CiviFormProfile profile = profileUtils.currentUserProfile(request);

                        AddressCorrectionBlockView.Params params =
                            AddressCorrectionBlockView.Params.builder()
                                .setRequest(request)
                                .setProfile(profile)
                                .setApplicantId(applicantId)
                                .setApplicantPersonalInfo(personalInfo)
                                .setMessages(messagesApi.preferred(request))
                                .setProgramId(programId)
                                .setProgramSlug(programSlugHandler.getProgramSlug(programParam))
                                .setProgramTitle(roApplicantProgramService.getProgramTitle())
                                .setProgramShortDescription(
                                    roApplicantProgramService.getProgramShortDescription())
                                .setBlockId(blockId)
                                .setBlockIndex(roApplicantProgramService.getBlockIndex(blockId))
                                .setBlockList(roApplicantProgramService.getAllActiveBlocks())
                                .setInReview(inReview)
                                .build();
                        return ok(addressCorrectionBlockView.render(
                                request,
                                params,
                                addressSuggestionGroup,
                                applicantRequestedActionWrapper.getAction(),
                                isEligibilityEnabledOnThisBlock,
                                addressSuggestionJson))
                            .as(Http.MimeTypes.HTML);
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
