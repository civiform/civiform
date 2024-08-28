package services.export;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.api.ApiPayloadWrapper;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import repository.ApplicationStatusesRepository;
import services.DeploymentType;
import services.Path;
import services.export.JsonExporterService.ApplicationExportData;
import services.export.enums.RevisionState;
import services.export.enums.SubmitterType;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;
import services.statuses.StatusDefinitions.Status;

/** Contains methods related to sampling JSON data for programs. */
public final class ProgramJsonSampler {

  private final QuestionJsonSampler.Factory questionJsonSamplerFactory;
  private final ApiPayloadWrapper apiPayloadWrapper;
  private final JsonExporterService jsonExporterService;
  private final DeploymentType deploymentType;
  private static final String EMPTY_VALUE = "";
  private final ApplicationStatusesRepository applicationStatusesRepository;

  @Inject
  ProgramJsonSampler(
      QuestionJsonSampler.Factory questionJsonSamplerFactory,
      ApiPayloadWrapper apiPayloadWrapper,
      JsonExporterService jsonExporterService,
      DeploymentType deploymentType,
      ApplicationStatusesRepository applicationStatusesRepository) {
    this.questionJsonSamplerFactory = questionJsonSamplerFactory;
    this.apiPayloadWrapper = apiPayloadWrapper;
    this.jsonExporterService = jsonExporterService;
    this.deploymentType = deploymentType;
    this.applicationStatusesRepository = applicationStatusesRepository;
  }

  /**
   * Samples JSON for a {@link ProgramDefinition} with fake data, appropriate for previews of what
   * the API response looks like.
   */
  public String getSampleJson(
      ProgramDefinition programDefinition, boolean multipleFileUploadEnabled) {
    ApplicationExportData.Builder jsonExportData =
        ApplicationExportData.builder()
            // Customizable program-specific API fields
            .setAdminName(programDefinition.adminName())
            .setStatus(
                applicationStatusesRepository
                    .lookupActiveStatusDefinitions(programDefinition.adminName())
                    .getStatuses()
                    .stream()
                    .findFirst()
                    .map(Status::statusText))
            .setProgramId(deploymentType.isDev() ? 789L : programDefinition.id())
            // Fields with arbitrary data.
            .setApplicantId(123L)
            .setApplicationId(456L)
            // Program ID changes on each browser test run, so set it to a constant
            // for those tests, otherwise to the actual program ID.
            .setLanguageTag(Locale.US.toLanguageTag())
            .setSubmitterType(SubmitterType.APPLICANT)
            .setRevisionState(RevisionState.CURRENT)
            .setTiEmail(EMPTY_VALUE)
            .setTiOrganization(EMPTY_VALUE)
            .setCreateTime(Instant.ofEpochSecond(1685047575)) // May 25, 2023 4:46 pm EDT
            .setSubmitTime(Instant.ofEpochSecond(1685133975)); // May 26, 2023 4:46 pm EDT

    ImmutableList<QuestionDefinition> questionDefinitions =
        programDefinition.streamQuestionDefinitions().collect(toImmutableList());

    for (QuestionDefinition questionDefinition : questionDefinitions) {
      @SuppressWarnings("unchecked")
      ImmutableMap<Path, Optional<?>> questionEntries =
          questionJsonSamplerFactory
              .create(questionDefinition.getQuestionType())
              .getSampleJsonEntries(questionDefinition, multipleFileUploadEnabled);

      jsonExportData.addApplicationEntries(questionEntries);
    }

    return apiPayloadWrapper.wrapPayload(
        jsonExporterService
            .convertApplicationExportDataListToJsonArray(
                ImmutableList.of(jsonExportData.build()), "{}")
            .jsonString(),
        /* paginationTokenPayload= */ Optional.empty());
  }
}
