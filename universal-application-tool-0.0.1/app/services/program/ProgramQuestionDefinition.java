package services.program;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.Optional;
import services.question.types.QuestionDefinition;

/**
 * {@link QuestionDefinition}s will not be stored in the database as part of the {@link
 * models.Program} model. Only the question id will be stored. It is up the {@link ProgramService}
 * to properly populate the question definition.
 */
@AutoValue
public abstract class ProgramQuestionDefinition {

  @JsonProperty("id")
  public abstract long id();

  /**
   * True if this program question definition is optional. Otherwise it is required.
   *
   * <p>This field was added in June 2021. Program question definitions created before this field will
   * default to required (false).
   */
  @JsonProperty("optional")
  public abstract boolean optional();

  abstract Optional<QuestionDefinition> questionDefinition();

  @JsonIgnore
  public QuestionDefinition getQuestionDefinition() {
    return questionDefinition().get();
  }

  @JsonIgnore
  public boolean hasQuestionDefinition() {
    return questionDefinition().isPresent();
  }

  @JsonCreator
  static ProgramQuestionDefinition create(
      @JsonProperty("id") long id, @JsonProperty("optional") boolean optional) {
    return new AutoValue_ProgramQuestionDefinition(id, optional, Optional.empty());
  }

  /** Create an optional program question definition. */
  public static ProgramQuestionDefinition create(QuestionDefinition questionDefinition) {
    return create(questionDefinition, true);
  }

  /** Create a program question definition. */
  public static ProgramQuestionDefinition create(
      QuestionDefinition questionDefinition, boolean optional) {
    return new AutoValue_ProgramQuestionDefinition(
        questionDefinition.getId(), optional, Optional.of(questionDefinition));
  }

  /** Return a program question definition with the {@link QuestionDefinition} set. */
  public ProgramQuestionDefinition setQuestionDefinition(QuestionDefinition questionDefinition) {
    return new AutoValue_ProgramQuestionDefinition(
        questionDefinition.getId(), optional(), Optional.of(questionDefinition));
  }

  /** Return a program question definition with a new optional setting. */
  public ProgramQuestionDefinition setOptional(boolean optional) {
    return create(getQuestionDefinition(), optional);
  }
}
