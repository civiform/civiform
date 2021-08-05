package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import services.Path;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.PresentsErrors;
import services.program.BlockDefinition;
import services.program.predicate.PredicateDefinition;
import services.question.types.ScalarType;

/**
 * Represents a block in the context of a specific user's application.
 *
 * <p>"Block" is synonymous to "screen", which is what we show in the admin UI. At some point, it
 * would be nice to rename the classes and methods to reflect the more desired name "screen".
 */
public final class Block {

  /**
   * The block's ID. Note this is different from the {@code BlockDefinition}'s ID because
   * BlockDefinitions that repeat expand to multiple Blocks.
   *
   * <p>A top level block has a single number, e.g. "1". This could be a {@link
   * services.question.types.QuestionType#ENUMERATOR} question (e.g. who are your household members)
   *
   * <p>Repeated blocks, which ask questions about repeated entities, use dash separated integers
   * which reference a block definition followed by repeated entity indices. For example, consider a
   * question about the incomes for jobs for each household member. Each applicant has multiple
   * household members, and each of those can have multiple jobs. Assume there is a {@link
   * BlockDefinition} with ID 8 that is asking for the income for each job. For block id "8-1-2",
   * the "8" refers to the {@link BlockDefinition} ID, and the "1-2" refers to the first household
   * member's second job.
   */
  private final String id;

  private final BlockDefinition blockDefinition;
  private final ApplicantData applicantData;
  private final Optional<RepeatedEntity> repeatedEntity;

  private Optional<ImmutableList<ApplicantQuestion>> questionsMemo = Optional.empty();
  private Optional<ImmutableMap<Path, ScalarType>> scalarsMemo = Optional.empty();

  Block(
      String id,
      BlockDefinition blockDefinition,
      ApplicantData applicantData,
      Optional<RepeatedEntity> repeatedEntity) {
    this.id = id;
    this.blockDefinition = checkNotNull(blockDefinition);
    this.applicantData = checkNotNull(applicantData);
    this.repeatedEntity = checkNotNull(repeatedEntity);
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return blockDefinition.name();
  }

  public String getDescription() {
    return blockDefinition.description();
  }

  public Optional<PredicateDefinition> getVisibilityPredicate() {
    return blockDefinition.visibilityPredicate();
  }

  /**
   * Returns the {@link RepeatedEntity} associated with this block, if this is a repeated block.
   * Otherwise, return empty.
   */
  public Optional<RepeatedEntity> getRepeatedEntity() {
    return repeatedEntity;
  }

  /** This block is an enumerator block if its {@link BlockDefinition} is an enumerator. */
  public boolean isEnumerator() {
    return blockDefinition.isEnumerator();
  }

  /** Get the enumerator {@link ApplicantQuestion} for this enumerator block. */
  public ApplicantQuestion getEnumeratorQuestion() {
    if (isEnumerator()) {
      return getQuestions().get(0);
    }
    throw new RuntimeException(
        "Only an enumerator block can have an enumeration question definition.");
  }

  /**
   * This block is a file upload block if its {@link BlockDefinition} contains a file upload
   * question.
   */
  public boolean isFileUpload() {
    return blockDefinition.isFileUpload();
  }

  public ImmutableList<ApplicantQuestion> getQuestions() {
    if (questionsMemo.isEmpty()) {
      this.questionsMemo =
          Optional.of(
              blockDefinition.programQuestionDefinitions().stream()
                  .map(
                      programQuestionDefinition ->
                          new ApplicantQuestion(
                              programQuestionDefinition, applicantData, repeatedEntity))
                  .collect(toImmutableList()));
    }
    return questionsMemo.get();
  }

  /**
   * Returns the {@link ScalarType} of the path if the path exists in this block. Returns empty if
   * the path does not exist.
   *
   * <p>For multi-select questions (like checkbox), we must append {@code []} to the field name so
   * that the Play framework allows multiple form keys with the same value. When updates are passed
   * in the request, they are of the format {@code contextualizedQuestionPath.selection[index]}.
   * However, the scalar path does not end in {@code []}, so we remove the array element information
   * here before checking the type.
   */
  public Optional<ScalarType> getScalarType(Path path) {
    if (path.isArrayElement()) {
      path = path.withoutArrayReference();
    }
    return Optional.ofNullable(getContextualizedScalars().get(path));
  }

  /**
   * Returns a map of contextualized {@link Path}s to all scalars (including metadata scalars) to
   * all questions in this block.
   */
  private ImmutableMap<Path, ScalarType> getContextualizedScalars() {
    if (scalarsMemo.isEmpty()) {
      scalarsMemo =
          Optional.of(
              getQuestions().stream()
                  .flatMap(question -> question.getContextualizedScalars().entrySet().stream())
                  .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
    return scalarsMemo.get();
  }

  /** A block has errors if any one of its {@link ApplicantQuestion}s has errors. */
  public boolean hasErrors() {
    return getQuestions().stream().anyMatch(ApplicantQuestion::hasErrors);
  }

  /**
   * Return true if any of this blocks questions are required but were left unanswered while filling
   * out the current program.
   */
  public boolean hasRequiredQuestionsThatAreUnansweredInCurrentProgram() {
    return getQuestions().stream()
        .anyMatch(ApplicantQuestion::isRequiredButWasUnansweredInCurrentProgram);
  }

  /**
   * Checks whether the block is complete - that is, {@link ApplicantData} has values at all the
   * paths for all required questions in this block and there are no errors. Note: this cannot be
   * memoized, since we need to reflect internal changes to ApplicantData.
   */
  public boolean isCompleteWithoutErrors() {
    // TODO(https://github.com/seattle-uat/civiform/issues/551): Stream only required scalar paths
    //  instead of all scalar paths.
    return isComplete() && !hasErrors();
  }

  /**
   * A block is complete if all of its {@link ApplicantQuestion}s {@link
   * PresentsErrors#isAnswered()}.
   */
  private boolean isComplete() {
    return getQuestions().stream()
        .map(ApplicantQuestion::errorsPresenter)
        .allMatch(PresentsErrors::isAnswered);
  }

  /**
   * Checks that this block is complete and that at least one of the questions was answered during
   * the program.
   *
   * @param programId the program ID to check
   * @return true if this block is complete at least one question was updated while filling out the
   *     program with the given ID; false if this block is incomplete; if it is complete with
   *     errors; or it is complete and all questions were answered in a different program.
   */
  public boolean wasCompletedInProgram(long programId) {
    return isCompleteWithoutErrors()
        && getQuestions().stream()
            .anyMatch(
                q -> {
                  Optional<Long> lastUpdatedInProgram = q.getUpdatedInProgramMetadata();
                  return lastUpdatedInProgram.isPresent()
                      && lastUpdatedInProgram.get().equals(programId);
                });
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof Block) {
      Block that = (Block) object;
      return this.id.equals(that.id)
          && this.blockDefinition.equals(that.blockDefinition)
          && this.applicantData.equals(that.applicantData);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, blockDefinition, applicantData);
  }

  /** Blocks are called "Screen"s in user facing UI. */
  @Override
  public String toString() {
    return "Screen [id: " + this.id + "]";
  }
}
