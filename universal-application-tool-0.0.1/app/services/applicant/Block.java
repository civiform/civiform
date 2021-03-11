package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.program.BlockDefinition;
import services.program.ProgramQuestionDefinition;

/** Represents a block in the context of a specific user's application. */
public final class Block {

  /**
   * The block's ID. Note this is different from the {@code BlockDefinition}'s ID because
   * BlockDefinitions that repeat expand to multiple Blocks.
   */
  private final long id;

  private final BlockDefinition blockDefinition;
  private final ApplicantData applicantData;
  // TODO: Make Block an AutoValue instead of implementing our own memoization.
  private Optional<ImmutableList<ApplicantQuestion>> questionsMemo = Optional.empty();
  private Optional<Boolean> errorsMemo = Optional.empty();

  Block(long id, BlockDefinition blockDefinition, ApplicantData applicantData) {
    this.id = id;
    this.blockDefinition = checkNotNull(blockDefinition);
    this.applicantData = checkNotNull(applicantData);
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return blockDefinition.name();
  }

  public String getDescription() {
    return blockDefinition.description();
  }

  public ImmutableList<ApplicantQuestion> getQuestions() {
    if (questionsMemo.isEmpty()) {
      this.questionsMemo =
          Optional.of(
              blockDefinition.programQuestionDefinitions().stream()
                  .map(ProgramQuestionDefinition::getQuestionDefinition)
                  .map(
                      questionDefinition ->
                          new ApplicantQuestion(questionDefinition, applicantData))
                  .collect(toImmutableList()));
    }
    return questionsMemo.get();
  }

  public boolean hasErrors() {
    if (errorsMemo.isEmpty()) {
      this.errorsMemo =
          getQuestions().stream().map(ApplicantQuestion::hasErrors).reduce(Boolean::logicalAnd);
    }
    return errorsMemo.get();
  }

  @Override
  public String toString() {
    return "Id: " + this.id;
  }
}
