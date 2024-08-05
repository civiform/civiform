package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import services.Path;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Question;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.predicate.LeafAddressServiceAreaExpressionNode;
import services.program.predicate.PredicateAddressServiceAreaNodeExtractor;
import services.program.predicate.PredicateDefinition;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionType;
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

  public String getLocalizedName(Locale preferredLocale) {
    return blockDefinition.localizedName().getOrDefault(preferredLocale);
  }

  public String getLocalizedDescription(Locale preferredLocale) {
    return blockDefinition.localizedDescription().getOrDefault(preferredLocale);
  }

  public Optional<EligibilityDefinition> getEligibilityDefinition() {
    return blockDefinition.eligibilityDefinition();
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

  /** This block is an address block if its {@link BlockDefinition} contains an address question. */
  public boolean hasAddress() {
    return blockDefinition.hasAddress();
  }

  /** This block is an address block if its {@link BlockDefinition} contains an address question. */
  public boolean hasAddressWithCorrectionEnabled() {
    return blockDefinition.hasAddressWithCorrectionEnabled();
  }

  /**
   * Returns a list of address service area IDs defined for eligibility on this block. Returns empty
   * if no service area IDs are configured for eligibility.
   */
  public Optional<ImmutableList<String>> getLeafAddressNodeServiceAreaIds() {
    Optional<EligibilityDefinition> eligibilityDefinition = getEligibilityDefinition();
    if (eligibilityDefinition.isEmpty()) {
      return Optional.empty();
    }

    ImmutableList<LeafAddressServiceAreaExpressionNode> nodes =
        PredicateAddressServiceAreaNodeExtractor.extract(eligibilityDefinition.get().predicate());

    return Optional.of(
        nodes.stream()
            .map(LeafAddressServiceAreaExpressionNode::serviceAreaId)
            .collect(ImmutableList.toImmutableList()));
  }

  /**
   * Returns a {@link ApplicantQuestion} that has address correction enabled if it exists. Returns
   * empty if no questions have address correction enabled.
   */
  public Optional<ApplicantQuestion> getAddressQuestionWithCorrectionEnabled() {
    return getQuestions().stream()
        .filter(ApplicantQuestion::isAddressCorrectionEnabled)
        .findFirst();
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

  public ApplicantQuestion getQuestion(Long id) throws QuestionNotFoundException {
    return getQuestions().stream()
        .filter(question -> question.getQuestionDefinition().getId() == id)
        .findFirst()
        .orElseThrow(() -> new QuestionNotFoundException(id));
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
   * Returns whether the block contains any static questions regardless of what other questions are
   * present and whether they are answered or not.
   */
  public boolean containsStatic() {
    return getQuestions().stream()
        .map(ApplicantQuestion::getType)
        .anyMatch(type -> type.equals(QuestionType.STATIC));
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

  /**
   * A block has errors if any one of its {@link ApplicantQuestion}s has errors or if there were any
   * failures to update the {@link ApplicantData}.
   */
  public boolean hasErrors() {
    return getQuestions().stream().anyMatch(ApplicantQuestion::hasErrors)
        || !applicantData.getFailedUpdates().isEmpty();
  }

  /**
   * Checks whether the block is answered - that is, {@link ApplicantData} has values at all the
   * paths for all questions in this block and there are no errors. Note: this cannot be memoized,
   * since we need to reflect internal changes to ApplicantData.
   *
   * <p>A block with optional questions that are not answered is not answered.
   */
  public boolean isAnsweredWithoutErrors() {
    return isAnswered() && !hasErrors();
  }

  /** A block is answered if all of its {@link ApplicantQuestion}s {@link Question#isAnswered()}. */
  private boolean isAnswered() {
    return getQuestions().stream().allMatch(ApplicantQuestion::isAnswered);
  }

  public int answeredQuestionsCount() {
    return (int) getQuestions().stream().filter(ApplicantQuestion::isAnswered).count();
  }

  /**
   * The number of questions answered by the applicant (excluding static content questions because
   * they have no fields for a user to answer)
   */
  public int answeredByUserQuestionsCount() {
    return (int)
        getQuestions().stream()
            .filter(question -> !question.getType().equals(QuestionType.STATIC))
            .filter(ApplicantQuestion::isAnswered)
            .count();
  }

  /**
   * The number of questions that can be answered by the applicant (excludes static content
   * questions).
   */
  public int answerableQuestionsCount() {
    return (int)
        getQuestions().stream()
            .filter(question -> !question.getType().equals(QuestionType.STATIC))
            .count();
  }

  /**
   * A block is complete with respect to a specific program if all of its questions are answered, or
   * are optional questions that were skipped in the program.
   */
  public boolean isCompletedInProgramWithoutErrors() {
    return isCompleteInProgram() && !hasErrors();
  }

  /**
   * A block is considered complete in a program if 1) It has no questions (this is a bit of a
   * bugfix hack so that empty blocks the admin hasn't added content to don't prevent applicants
   * from being able to submit their applications). OR 2) Each of its required questions is answered
   * AND each of its optional questions is answered or intentionally skipped.
   */
  private boolean isCompleteInProgram() {
    return getQuestions().isEmpty()
        || getQuestions().stream()
            .allMatch(ApplicantQuestion::isAnsweredOrSkippedOptionalInProgram);
  }

  /** Returns true if this block contains only optional questions and false otherwise. */
  public boolean hasOnlyOptionalQuestions() {
    return getQuestions().stream().allMatch(ApplicantQuestion::isOptional);
  }

  /**
   * Checks that this block is answered and that at least one of the questions was answered during
   * the program.
   *
   * @param programId the program ID to check
   * @return true if this block is complete at least one question was updated while filling out the
   *     program with the given ID; false if this block has unanswered questions, has errors, or if
   *     all of its questions were answered in a different program.
   */
  public boolean wasAnsweredInProgram(long programId) {
    return isAnsweredWithoutErrors()
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
