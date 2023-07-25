package services.export;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static services.export.JsonExporter.exportEntriesToJsonApplication;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import javax.inject.Inject;
import services.CfJsonDocumentContext;
import services.Path;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;

/** Contains methods related to sampling JSON data for programs. */
public final class ProgramJsonSampler {

  private final QuestionJsonSampler.Factory questionJsonSamplerFactory;

  @Inject
  ProgramJsonSampler(QuestionJsonSampler.Factory questionJsonSamplerFactory) {
    this.questionJsonSamplerFactory = questionJsonSamplerFactory;
  }

  /**
   * Samples JSON for a {@link ProgramDefinition} with fake data, appropriate for previews of what
   * the API response looks like.
   */
  public CfJsonDocumentContext getSampleJson(ProgramDefinition programDefinition) {
    ImmutableList<QuestionDefinition> questionDefinitions =
        programDefinition.streamQuestionDefinitions().collect(toImmutableList());

    CfJsonDocumentContext sampleJson = new CfJsonDocumentContext();
    for (QuestionDefinition questionDefinition : questionDefinitions) {
      @SuppressWarnings("unchecked")
      ImmutableMap<Path, Optional<?>> questionEntries =
          questionJsonSamplerFactory
              .create(questionDefinition.getQuestionType())
              .getSampleJsonEntries(questionDefinition);

      exportEntriesToJsonApplication(sampleJson, questionEntries);
    }

    return sampleJson;
  }
}
