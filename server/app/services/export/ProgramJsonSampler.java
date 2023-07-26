package services.export;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import services.CfJsonDocumentContext;
import services.DeploymentType;
import services.Path;
import services.export.JsonExporter.JsonExportData;
import services.program.ProgramDefinition;
import services.program.StatusDefinitions.Status;
import services.question.types.QuestionDefinition;

/** Contains methods related to sampling JSON data for programs. */
public final class ProgramJsonSampler {

  private final QuestionJsonSampler.Factory questionJsonSamplerFactory;
  private final JsonExporter jsonExporter;
  private final DeploymentType deploymentType;

  @Inject
  ProgramJsonSampler(
      QuestionJsonSampler.Factory questionJsonSamplerFactory,
      JsonExporter jsonExporter,
      DeploymentType deploymentType) {
    this.questionJsonSamplerFactory = questionJsonSamplerFactory;
    this.jsonExporter = jsonExporter;
    this.deploymentType = deploymentType;
  }

  /**
   * Samples JSON for a {@link ProgramDefinition} with fake data, appropriate for previews of what
   * the API response looks like.
   */
  public CfJsonDocumentContext getSampleJson(ProgramDefinition programDefinition) {
    JsonExportData.Builder jsonExportData =
        JsonExportData.builder()
            // Customizable program-specific API fields
            .setAdminName(programDefinition.adminName())
            .setStatus(
                programDefinition.statusDefinitions().getStatuses().stream()
                    .findFirst()
                    .map(Status::statusText))
            .setProgramId(deploymentType.isDev() ? 789L : programDefinition.id())
            // Fields with arbitrary data.
            .setApplicantId(123L)
            .setApplicationId(456L)
            // Program ID changes on each browser test run, so set it to a constant
            // for those tests, otherwise to the actual program ID.
            .setLanguageTag(Locale.US.toLanguageTag())
            .setSubmitterEmail("homer.simpson@springfield.gov")
            .setCreateTime(Instant.ofEpochSecond(1685047575)) // May 25, 2023 4:46 pm EDT
            .setSubmitTime(Instant.ofEpochSecond(1685133975)); // May 26, 2023 4:46 pm EDT

    ImmutableList<QuestionDefinition> questionDefinitions =
        programDefinition.streamQuestionDefinitions().collect(toImmutableList());

    for (QuestionDefinition questionDefinition : questionDefinitions) {
      @SuppressWarnings("unchecked")
      ImmutableMap<Path, Optional<?>> questionEntries =
          questionJsonSamplerFactory
              .create(questionDefinition.getQuestionType())
              .getSampleJsonEntries(questionDefinition);

      jsonExportData.addApplicationEntries(questionEntries);
    }

    return jsonExporter.buildJsonApplication(jsonExportData.build());
  }
}
