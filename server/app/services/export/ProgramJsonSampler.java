package services.export;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import controllers.api.ApiPayloadWrapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import repository.ApplicationStatusesRepository;
import services.DeploymentType;
import services.Path;
import services.applicant.ApplicantData;
import services.export.JsonExporterService.ApplicationExportData;
import services.export.QuestionJsonSampler.SampleDataContext;
import services.export.enums.RevisionState;
import services.export.enums.SubmitterType;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
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
   * the API response looks like, without answer-option scores.
   */
  public String getSampleJson(ProgramDefinition programDefinition) {
    return getSampleJson(programDefinition, /* includeScores= */ false);
  }

  /**
   * Samples JSON for a {@link ProgramDefinition} with fake data, appropriate for previews of what
   * the API response looks like.
   *
   * @param includeScores whether the answer-option-scoring feature flag is on for this request;
   *     when true, supported questions gain sample {@code score}/{@code scores} values and the
   *     response gains a {@code total_score}
   */
  public String getSampleJson(ProgramDefinition programDefinition, boolean includeScores) {
    ApplicationExportData.Builder jsonExportData =
        ApplicationExportData.builder()
            // Customizable program-specific API fields
            .setAdminName(programDefinition.adminName())
            .setApplicationNote(Optional.empty())
            .setStatus(
                applicationStatusesRepository
                    .lookupActiveStatusDefinitions(programDefinition.adminName())
                    .getStatuses()
                    .stream()
                    .findFirst()
                    .map(Status::statusText))
            .setStatusLastModifiedTime(Optional.empty())
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

    SampleDataContext sampleDataContext = new SampleDataContext();

    double sampleTotalScore = 0;
    for (QuestionDefinition questionDefinition : questionDefinitions) {
      @SuppressWarnings("unchecked")
      ImmutableMap<Path, Optional<?>> questionEntries =
          questionJsonSamplerFactory
              .create(questionDefinition.getQuestionType())
              .getSampleJsonEntries(questionDefinition, sampleDataContext);

      jsonExportData.addApplicationEntries(questionEntries);

      // Sample score values for supported non-repeated question types; repeated sample questions
      // are left unscored.
      if (includeScores
          && QuestionType.supportsOptionScores(questionDefinition.getQuestionType())
          && questionDefinition.getEnumeratorId().isEmpty()) {
        Path apiPath =
            ApplicantData.APPLICANT_PATH.join(questionDefinition.getQuestionPathSegment());
        if (questionDefinition.getQuestionType() == QuestionType.CHECKBOX) {
          // One scored (fractional, demonstrating decimals) and one unscored (null) sample
          // selection.
          List<Double> sampleScores = new ArrayList<>();
          sampleScores.add(1.5);
          sampleScores.add(null);
          jsonExportData.addApplicationEntries(
              ImmutableMap.<Path, Optional<?>>of(
                  apiPath.join("scores"), Optional.of(new NullableDoubleArray(sampleScores))));
          sampleTotalScore += 1.5;
        } else {
          jsonExportData.addApplicationEntries(
              ImmutableMap.<Path, Optional<?>>of(apiPath.join("score"), Optional.of(2.0)));
          sampleTotalScore += 2;
        }
      }
    }
    if (includeScores) {
      jsonExportData.setTotalScore(Optional.of(sampleTotalScore));
    }

    return apiPayloadWrapper.wrapPayload(
        jsonExporterService
            .convertApplicationExportDataListToJsonArray(
                ImmutableList.of(jsonExportData.build()),
                "{}",
                includeScores,
                /* checkboxScoreApiPaths= */ ImmutableSet.of())
            .jsonString(),
        /* paginationTokenPayload= */ Optional.empty());
  }
}
