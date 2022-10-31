package services;

import java.util.Optional;
import services.program.BlockDefinition;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;

/** Helper class for performing validation related to creating or modifying program blocks. */
public final class ProgramBlockValidation {

  private ProgramBlockValidation() {}

  /**
   * Result of checking whether a question can be added to a specific blocks. Only ELIGIBLE means
   * that question can be adde. All other states indicate that question is not eligible for the
   * given block.
   */
  public enum AddQuestionResult {
    // Question can be added to the block.
    ELIGIBLE,

    // Question already exists in one of the blocks of the given program.
    DUPLICATE,

    // Cannot add question to the block because the block contains a question
    // of special type (enumerator or file upload) that prohibits having
    // other questions in the block.
    BLOCK_IS_SINGLE_QUESTION,

    // Cannot add special question type (enumerator or file upload) to
    // non-empty block. These questions must be an only question in a block.
    CANT_ADD_SINGLE_BLOCK_QUESTION_TO_NON_EMPTY_BLOCK,

    // Cannot add question to the block because either block is a child of enumerator
    // block and can contain only questions that themselves children of the enumerator and
    // provided question is not. Or provided question is a child of enumerator question while
    // the block is a regular block.
    ENUMERATOR_MISMATCH
  }

  /**
   * Check whether given question can be added to the provided block. This should be checked both
   * during rendering to make sure admins don't see ineligible questions when creating blocks. Also
   * it should be checked during the actual server-side block creating to ensure that no one can
   * maliciously mess up data by sending specially crafted request (or in case we mess up
   * client-side checks).
   */
  public static AddQuestionResult canAddQuestion(
      ProgramDefinition program, BlockDefinition block, QuestionDefinition question) {
    if (program.hasQuestion(question)) {
      return AddQuestionResult.DUPLICATE;
    }
    if (block.isEnumerator() || block.isFileUpload()) {
      return AddQuestionResult.BLOCK_IS_SINGLE_QUESTION;
    }
    if (block.getQuestionCount() > 0 && ProgramBlockValidation.isSingleBlockQuestion(question)) {
      return AddQuestionResult.CANT_ADD_SINGLE_BLOCK_QUESTION_TO_NON_EMPTY_BLOCK;
    }
    if (!question
        .getEnumeratorId()
        .equals(ProgramBlockValidation.getEnumeratorQuestionId(program, block))) {
      return AddQuestionResult.ENUMERATOR_MISMATCH;
    }
    return AddQuestionResult.ELIGIBLE;
  }

  private static boolean isSingleBlockQuestion(QuestionDefinition question) {
    switch (question.getQuestionType()) {
      case ENUMERATOR:
      case FILEUPLOAD:
        return true;
      default:
        return false;
    }
  }

  /**
   * Follow the {@link BlockDefinition#enumeratorId()} reference to the enumerator block definition,
   * and return the id of its {@link EnumeratorQuestionDefinition}.
   */
  private static Optional<Long> getEnumeratorQuestionId(
      ProgramDefinition program, BlockDefinition block) {
    if (block.enumeratorId().isEmpty()) {
      return Optional.empty();
    }
    try {
      BlockDefinition enumeratorBlockDefinition =
          program.getBlockDefinition(block.enumeratorId().get());
      return Optional.of(enumeratorBlockDefinition.getQuestionDefinition(0).getId());
    } catch (ProgramBlockDefinitionNotFoundException e) {
      String errorMessage =
          String.format(
              "BlockDefinition %d has a broken enumerator block reference to id %d",
              block.id(), block.enumeratorId().get());
      throw new RuntimeException(errorMessage, e);
    }
  }
}
