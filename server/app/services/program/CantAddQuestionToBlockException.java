package services.program;

import services.ProgramBlockValidation;
import services.question.types.QuestionDefinition;

public class CantAddQuestionToBlockException extends Exception {
  public CantAddQuestionToBlockException(
      ProgramDefinition program,
      BlockDefinition block,
      QuestionDefinition question,
      ProgramBlockValidation.AddQuestionResult reason) {
    super(
        String.format(
            "Can't add question to the block. Error: %s. Check comments in AddQuestionResult enum."
                + " Program ID %d, block ID %d, question ID %d",
            reason, program.id(), block.id(), question.getId()));
  }

  /** Message that can be returned in error page that doesn't contain internal info. */
  public String externalMessage() {
    // Message doesn't contain any private info. Program, block, question ids are not sensitive.
    return this.getMessage();
  }
}
