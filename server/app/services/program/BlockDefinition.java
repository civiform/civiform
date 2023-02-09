package services.program;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.program.predicate.LeafAddressServiceAreaExpressionNode;
import services.program.predicate.PredicateAddressServiceAreaNodeExtractor;
import services.program.predicate.PredicateDefinition;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

/**
 * Defines a single program block, which contains a list of questions and data about the block.
 * Blocks are displayed to applicants one per-page, and are the primary means by which applicants
 * navigate within a program form.
 *
 * <p>"Block" is synonymous to "screen", which is what we show in the admin UI. At some point, it
 * would be nice to rename the classes and methods to reflect the more desired name "screen".
 */
@JsonDeserialize(builder = AutoValue_BlockDefinition.Builder.class)
@AutoValue
public abstract class BlockDefinition {

  public static Builder builder() {
    return new AutoValue_BlockDefinition.Builder();
  }

  /**
   * A block identifier. Only unique between current blocks within a {@link ProgramDefinition}.
   *
   * <p>Blocks from one ProgramDefinition may have the same Ids as blocks from another
   * ProgramDefinition. Blocks that are deleted from a ProgramDefinition may have its Id reused.
   */
  @JsonProperty("id")
  public abstract long id();

  /**
   * Name of a Block used to identify it to the admin. The name is only visible to the admin so is
   * not localized.
   */
  @JsonProperty("name")
  public abstract String name();

  /**
   * A human readable description of the Block. The description is only visible to the admin so is
   * not localized.
   */
  @JsonProperty("description")
  public abstract String description();

  /**
   * An enumerator block definition is a block definition that contains a {@link QuestionDefinition}
   * that is of type {@link QuestionType#ENUMERATOR}. Enumerator questions provide a variable list
   * of user-defined identifiers for some repeated entity. Examples of repeated entities could be
   * household members, vehicles, jobs, etc.
   *
   * <p>An enumerator block can only have one question, and it must be {@link
   * QuestionType#ENUMERATOR}.
   *
   * @return true if this block definition is an enumerator.
   */
  @JsonIgnore
  @Memoized
  public boolean isEnumerator() {
    // Though `anyMatch` is used here, enumerator block definitions should only ever have a single
    // question, which is an enumerator question.
    return programQuestionDefinitions().stream()
        .map(ProgramQuestionDefinition::getQuestionDefinition)
        .map(QuestionDefinition::getQuestionType)
        .anyMatch(questionType -> questionType.equals(QuestionType.ENUMERATOR));
  }

  /**
   * A repeated block definition references an enumerator block definition that determines the
   * entities the repeated block definition asks questions for. If a block definition does not have
   * a enumeratorId, it is not repeated.
   *
   * @return the BlockDefinition ID for this block definitions enumerator, if it exists
   */
  // TODO(https://github.com/seattle-uat/civiform/issues/993): migrate and then rename repeaterId
  //  to enumeratorId
  @JsonProperty("repeaterId")
  public abstract Optional<Long> enumeratorId();

  /**
   * See {@link #enumeratorId()}.
   *
   * @return true if this block definition is for a repeated block.
   */
  @JsonIgnore
  public boolean isRepeated() {
    return enumeratorId().isPresent();
  }

  @JsonIgnore
  public EnumeratorQuestionDefinition getEnumerationQuestionDefinition() {
    if (isEnumerator()) {
      return (EnumeratorQuestionDefinition) getQuestionDefinition(0);
    }
    throw new RuntimeException(
        "Only an enumerator block can have an enumeration question definition.");
  }

  @JsonIgnore
  @Memoized
  public boolean isFileUpload() {
    // Though `anyMatch` is used here, fileupload block definitions should only ever have a single
    // question, which is a fileupload question.
    return programQuestionDefinitions().stream()
        .map(ProgramQuestionDefinition::getQuestionDefinition)
        .map(QuestionDefinition::getQuestionType)
        .anyMatch(questionType -> questionType.equals(QuestionType.FILEUPLOAD));
  }

  @JsonIgnore
  @Memoized
  public boolean hasAddress() {
    return programQuestionDefinitions().stream()
        .map(ProgramQuestionDefinition::getQuestionDefinition)
        .map(QuestionDefinition::getQuestionType)
        .anyMatch(questionType -> questionType.equals(QuestionType.ADDRESS));
  }

  /**
   * Check if block already contains a question with address correction enabled, ignoring the given
   * question ID that you are trying to enable it on.
   */
  @JsonIgnore
  public boolean hasAddressCorrectionEnabledOnDifferentQuestion(long questionDefinitionId) {
    return programQuestionDefinitions().stream()
        .anyMatch(
            question ->
                question.addressCorrectionEnabled() && question.id() != questionDefinitionId);
  }

  /** A {@link PredicateDefinition} that determines whether this block is hidden or shown. */
  @JsonProperty("hidePredicate")
  public abstract Optional<PredicateDefinition> visibilityPredicate();

  /**
   * An {@link EligibilityDefinition} that determines whether this block can be continued on from or
   * not.
   *
   * <p>This contains a {@link PredicateDefinition} that determines if the applicant is eligible or
   * not for the program as of this block.
   */
  @JsonInclude(Include.NON_EMPTY)
  @JsonProperty("eligibilityDefinition")
  public abstract Optional<EligibilityDefinition> eligibilityDefinition();

  /**
   * True if the block has an eligibility or visibility predicate containing one or more {@link
   * LeafAddressServiceAreaExpressionNode}.
   */
  @JsonIgnore
  @Memoized
  public boolean hasAddressServiceAreaPredicateNodes() {
    return !getAddressServiceAreaPredicateNodes().isEmpty();
  }

  /**
   * Returns all {@link LeafAddressServiceAreaExpressionNode}s in the block's eligibility and
   * visibility predicates.
   */
  @JsonIgnore
  @Memoized
  public ImmutableList<LeafAddressServiceAreaExpressionNode> getAddressServiceAreaPredicateNodes() {
    ImmutableList.Builder<LeafAddressServiceAreaExpressionNode> result = ImmutableList.builder();

    eligibilityDefinition()
        .map(EligibilityDefinition::predicate)
        .map(PredicateAddressServiceAreaNodeExtractor::extract)
        .ifPresent(result::addAll);

    visibilityPredicate()
        .map(PredicateAddressServiceAreaNodeExtractor::extract)
        .ifPresent(result::addAll);

    return result.build();
  }

  /**
   * A {@link PredicateDefinition} that determines whether this is optional or required.
   *
   * <p>Note as of 2021-05-25: We no longer consider blocks to be required or optional - a block is
   * required if shown. Instead, individual questions can be optional or required. This field is
   * kept for serialization consistency.
   */
  @JsonProperty("optionalPredicate")
  public abstract Optional<PredicateDefinition> optionalPredicate();

  /** The list of {@link ProgramQuestionDefinition}s that make up this block. */
  @JsonProperty("questionDefinitions")
  public abstract ImmutableList<ProgramQuestionDefinition> programQuestionDefinitions();

  public abstract Builder toBuilder();

  /** Returns the {@link QuestionDefinition} at the specified index. */
  @JsonIgnore
  public QuestionDefinition getQuestionDefinition(int questionIndex) {
    return programQuestionDefinitions().get(questionIndex).getQuestionDefinition();
  }

  @JsonIgnore
  @Memoized
  public int getQuestionCount() {
    return programQuestionDefinitions().size();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    @JsonProperty("id")
    public abstract Builder setId(long id);

    @JsonProperty("name")
    public abstract Builder setName(String value);

    @JsonProperty("description")
    public abstract Builder setDescription(String value);

    // TODO(https://github.com/seattle-uat/civiform/issues/993): migrate and then rename repeaterId
    //  to enumeratorId
    @JsonProperty("repeaterId")
    public abstract Builder setEnumeratorId(Optional<Long> enumeratorId);

    @JsonProperty("hidePredicate")
    public abstract Builder setVisibilityPredicate(Optional<PredicateDefinition> predicate);

    public Builder setVisibilityPredicate(PredicateDefinition predicate) {
      return this.setVisibilityPredicate(Optional.of(predicate));
    }

    @JsonProperty("eligibilityDefinition")
    public abstract Builder setEligibilityDefinition(Optional<EligibilityDefinition> eligibility);

    public Builder setEligibilityDefinition(EligibilityDefinition eligibility) {
      return this.setEligibilityDefinition(Optional.of(eligibility));
    }

    @JsonProperty("optionalPredicate")
    public abstract Builder setOptionalPredicate(Optional<PredicateDefinition> optional);

    @JsonProperty("questionDefinitions")
    public abstract Builder setProgramQuestionDefinitions(
        ImmutableList<ProgramQuestionDefinition> programQuestionDefinitions);

    public abstract ImmutableList.Builder<ProgramQuestionDefinition>
        programQuestionDefinitionsBuilder();

    public abstract BlockDefinition build();

    public Builder addQuestion(ProgramQuestionDefinition question) {
      programQuestionDefinitionsBuilder().add(question);
      return this;
    }
  }
}
