package services.applicant;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.program.BlockDefinition;
import services.program.ProgramQuestionDefinition;

/** Represents a block in the context of a specific user's application. */
@AutoValue
public abstract class Block {

  public static Block create(
      long id, BlockDefinition blockDefinition, ApplicantData applicantData) {
    return new AutoValue_Block(id, blockDefinition, applicantData);
  }

  /**
   * The block's ID. Note this is different from the {@code BlockDefinition}'s ID because
   * BlockDefinitions that repeat expand to multiple Blocks.
   */
  public abstract long id();

  abstract BlockDefinition blockDefinition();

  /** Note: This is a mutable type - the underlying JSON can change. */
  abstract ApplicantData applicantData();

  @Memoized
  public String getName() {
    return blockDefinition().name();
  }

  @Memoized
  public String getDescription() {
    return blockDefinition().description();
  }

  @Memoized
  public ImmutableList<ApplicantQuestion> getQuestions() {
    return blockDefinition().programQuestionDefinitions().stream()
        .map(ProgramQuestionDefinition::getQuestionDefinition)
        .map(questionDefinition -> new ApplicantQuestion(questionDefinition, applicantData()))
        .collect(toImmutableList());
  }

  /** Note - this cannot be memoized since ApplicantData may change. */
  public boolean hasErrors() {
    Optional<Boolean> hasErrors =
        getQuestions().stream().map(ApplicantQuestion::hasErrors).reduce(Boolean::logicalOr);
    return hasErrors.isPresent() ? hasErrors.get() : false;
  }

  /**
   * Checks whether the block is complete - that is, {@link ApplicantData} has values at all the
   * paths for all required questions in this block. Note: this cannot be memoized, since we need to
   * reflect internal changes to ApplicantData.
   */
  public boolean isCompleteWithoutErrors() {
    return blockDefinition().scalarPaths().stream()
        .filter(path -> !applicantData().hasPath(path))
        .findAny()
        .isEmpty();
  }
}
