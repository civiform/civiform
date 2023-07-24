package services.export;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import javax.inject.Inject;
import services.CfJsonDocumentContext;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;

public class ProgramJsonSampler {

  private final QuestionJsonSampler.Factory questionJsonSamplerFactory;

  @Inject
  ProgramJsonSampler(QuestionJsonSampler.Factory questionJsonSamplerFactory) {
    this.questionJsonSamplerFactory = questionJsonSamplerFactory;
  }

  public CfJsonDocumentContext getSampleJson(ProgramDefinition programDefinition) {
    ImmutableList<QuestionDefinition> questionDefinitions =
        programDefinition.streamQuestionDefinitions().collect(toImmutableList());

    CfJsonDocumentContext sampleJson = new CfJsonDocumentContext();
    for (QuestionDefinition questionDefinition : questionDefinitions) {
      sampleJson.mergeFrom(
          questionJsonSamplerFactory
              .create(questionDefinition.getQuestionType())
              .getSampleJson(questionDefinition));
    }

    return sampleJson;
  }
}
