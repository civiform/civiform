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
   * True if this program question definition is required. Otherwise it is optional.
   *
   * <p>This field was added in June. Program question definitions created before this field will
   * default to optional (false).
   */
  @JsonProperty("required")
  public abstract boolean required();

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
      @JsonProperty("id") long id, @JsonProperty("required") boolean required) {
    return new AutoValue_ProgramQuestionDefinition(id, required, Optional.empty());
  }

  /** Create an optional program question definition. */
  public static ProgramQuestionDefinition create(QuestionDefinition questionDefinition) {
    return create(questionDefinition, false);
  }

  /** Create a program question definition. */
  public static ProgramQuestionDefinition create(
      QuestionDefinition questionDefinition, boolean required) {
    return new AutoValue_ProgramQuestionDefinition(
        questionDefinition.getId(), required, Optional.of(questionDefinition));
  }

  /** Return a program question definition with the {@link QuestionDefinition} set. */
  public ProgramQuestionDefinition setQuestionDefinition(QuestionDefinition questionDefinition) {
    return new AutoValue_ProgramQuestionDefinition(
        questionDefinition.getId(), required(), Optional.of(questionDefinition));
  }

  /** Return a program question definition with a new required setting. */
  public ProgramQuestionDefinition setRequired(boolean required) {
    return create(getQuestionDefinition(), required);
  }
}
