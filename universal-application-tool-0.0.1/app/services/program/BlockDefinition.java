package services.program;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import services.Path;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.question.types.ScalarType;

/**
 * Defines a single program block, which contains a list of questions and data about the block.
 * Blocks are displayed to applicants one per-page, and are the primary means by which applicants
 * navigate within a program form.
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
   * A repeater block definition is a block definition that contains a {@link QuestionDefinition}
   * that is of type {@code QuestionType.REPEATER}. Repeater questions provide a variable list of
   * user-defined identifiers for some repeated entity. Examples of repeated entities could be
   * household members, vehicles, jobs, etc.
   *
   * A repeater block can only have one question, and it must be {@link QuestionType#REPEATER}.
   *
   * @return true if this block definition is a repeater.
   */
  @JsonIgnore
  @Memoized
  public boolean isRepeater() {
    // Though `anyMatch` is used here, repeater block definitions should only ever have a single
    // question, which is a repeater question.
    return programQuestionDefinitions().stream()
        .map(ProgramQuestionDefinition::getQuestionDefinition)
        .map(QuestionDefinition::getQuestionType)
        .anyMatch(questionType -> questionType.equals(QuestionType.REPEATER));
  }

  /**
   * A repeated block definition has a reference to the {@link
   * services.question.types.RepeaterQuestionDefinition} whose repeated questions it can contain.
   */
  @JsonProperty("repeaterQuestionId")
  public abstract Optional<Long> repeaterQuestionId();

  /**
   * A repeated block definition references a repeater block definition that determines the entities
   * the repeated block definition asks questions for. If a block definition does not have a
   * repeaterId, it is not repeated.
   *
   * @return the BlockDefinition ID for this block definitions repeater, if it exists
   */
  @JsonProperty("repeaterId")
  public abstract Optional<Long> repeaterId();

  /**
   * See {@link #repeaterId()}.
   *
   * @return true if this block definition is for a repeated block.
   */
  @JsonIgnore
  public boolean isRepeated() {
    return repeaterId().isPresent() && repeaterQuestionId().isPresent();
  }

  /** A {@link Predicate} that determines whether this is hidden or shown. */
  @JsonProperty("hidePredicate")
  public abstract Optional<Predicate> hidePredicate();

  /** A {@link Predicate} that determines whether this is optional or required. */
  @JsonProperty("optionalPredicate")
  public abstract Optional<Predicate> optionalPredicate();

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

  @JsonIgnore
  @Memoized
  public ImmutableMap<Path, ScalarType> scalarTypes() {
    ImmutableMap.Builder<Path, ScalarType> scalarTypesBuilder = ImmutableMap.builder();
    programQuestionDefinitions().stream()
        .map(ProgramQuestionDefinition::getQuestionDefinition)
        .map(QuestionDefinition::getScalars)
        .forEach(scalarTypesBuilder::putAll);

    return scalarTypesBuilder.build();
  }

  @JsonIgnore
  @Memoized
  public ImmutableSet<Path> scalarPaths() {
    return scalarTypes().keySet();
  }

  /**
   * For multi-select questions (like checkbox), we must append {@code []} to the field name so that
   * the Play framework allows multiple form keys with the same value. When updates are passed in
   * the request, they are of the format {@code path.selection[index]}. However, the scalar path
   * does not end in {@code []}, so we remove the array element information here before checking the
   * type.
   */
  @JsonIgnore
  public Optional<ScalarType> getScalarType(Path path) {
    if (path.isArrayElement()) {
      path = path.withoutArrayReference();
    }
    return Optional.ofNullable(scalarTypes().get(path));
  }

  @JsonIgnore
  public boolean hasPaths(List<Path> paths) {
    return scalarPaths().containsAll(ImmutableSet.copyOf(paths));
  }

  @JsonIgnore
  public boolean hasPaths(Path... paths) {
    return hasPaths(ImmutableList.copyOf(paths));
  }

  @JsonIgnore
  public boolean hasSameId(BlockDefinition other) {
    return other.id() == id();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    @JsonProperty("id")
    public abstract Builder setId(long id);

    @JsonProperty("name")
    public abstract Builder setName(String value);

    @JsonProperty("description")
    public abstract Builder setDescription(String value);

    @JsonProperty("repeaterId")
    public abstract Builder setRepeaterId(Optional<Long> repeaterId);

    @JsonProperty("repeaterQuestionId")
    public abstract Builder setRepeaterQuestionId(Optional<Long> repeaterQuestionId);

    @JsonProperty("hidePredicate")
    public abstract Builder setHidePredicate(Optional<Predicate> hide);

    public Builder setHidePredicate(Predicate hide) {
      return this.setHidePredicate(Optional.of(hide));
    }

    @JsonProperty("optionalPredicate")
    public abstract Builder setOptionalPredicate(Optional<Predicate> optional);

    public Builder setOptionalPredicate(Predicate optional) {
      return this.setOptionalPredicate(Optional.of(optional));
    }

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
