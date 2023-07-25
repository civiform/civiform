package services.export;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import services.CfJsonDocumentContext;
import services.Path;
import services.export.JsonExporter.JsonExportData;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;

/** Contains methods related to sampling JSON data for programs. */
public final class ProgramJsonSampler {

  private final QuestionJsonSampler.Factory questionJsonSamplerFactory;
  private final JsonExporter jsonExporter;

  @Inject
  ProgramJsonSampler(
      QuestionJsonSampler.Factory questionJsonSamplerFactory, JsonExporter jsonExporter) {
    this.questionJsonSamplerFactory = questionJsonSamplerFactory;
    this.jsonExporter = jsonExporter;
  }

  /**
   * Samples JSON for a {@link ProgramDefinition} with fake data, appropriate for previews of what
   * the API response looks like.
   */
  public CfJsonDocumentContext getSampleJson(ProgramDefinition programDefinition) {
    JsonExportData.Builder jsonExportData =
        JsonExportData.builder()
            .setAdminName("admin name")
            .setApplicantId(123L)
            .setApplicationId(456L)
            .setProgramId(789L)
            .setLanguageTag(Locale.US.toLanguageTag())
            .setCreateTime(Instant.ofEpochSecond(1685047575)) // May 25, 2023 4:46 pm EDT
            .setSubmitterEmail("homer.simpson@springfield.gov")
            .setSubmitTimeOpt(Instant.ofEpochSecond(1685133975)) // May 26, 2023 4:46 pm EDT
            .setStatusOpt(Optional.of("current-status"));

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
