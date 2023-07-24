package services;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import services.ProgramBlockValidation.AddQuestionResult;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;

public class ProgramBlockValidationFactory {

  private final ProgramBlockValidation programBlockValidation;

  @Inject
  public ProgramBlockValidationFactory(ProgramBlockValidation programBlockValidation) {
    this.programBlockValidation = checkNotNull(programBlockValidation);
  }

  public AddQuestionResult canAddQuestion(
      ProgramDefinition program, BlockDefinition block, QuestionDefinition question) {
    return programBlockValidation.canAddQuestion(program, block, question);
  }
}
