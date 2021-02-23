package services.program;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.Optional;
import services.question.QuestionDefinition;

/**
 * {@link QuestionDefinition}s will not be stored in the database as part of the {@link
 * models.Program} model. Only the question id will be stored. It is up the {@link ProgramService}
 * to properly populate the question definition.
 */
@AutoValue
public abstract class ProgramQuestionDefinition {

  @JsonProperty("id")
  public abstract long id();

  @JsonIgnore
  abstract Optional<QuestionDefinition> questionDefinition();

  @JsonIgnore
  public QuestionDefinition getQuestionDefinition() {
    return questionDefinition().get();
  }

  @JsonCreator
  static ProgramQuestionDefinition create(@JsonProperty("id") long id) {
    return new AutoValue_ProgramQuestionDefinition(id, Optional.empty());
  }

  public static ProgramQuestionDefinition create(QuestionDefinition questionDefinition) {
    return new AutoValue_ProgramQuestionDefinition(
        questionDefinition.getId(), Optional.of(questionDefinition));
  }
}
