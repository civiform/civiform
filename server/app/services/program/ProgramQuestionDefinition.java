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

  /** The ID of the wrapped {@link QuestionDefinition}. */
  @JsonProperty("id")
  public abstract long id();

  /**
   * A reference to the program this is a question of. Program question definitions are not stored
   * with this reference and this should be set upon load.
   */
  abstract Optional<Long> programDefinitionId();

  /**
   * True if this program question definition is optional. Otherwise it is required.
   *
   * <p>This field was added in June 2021. Program question definitions created before this field
   * will default to required (false).
   */
  @JsonProperty("optional")
  public abstract boolean optional();

  /**
   * The actual question with same id as this program question definition. Program question
   * definitions are not stored with this value, so it may not be present unless it is explicitly
   * loaded in.
   */
  abstract Optional<QuestionDefinition> questionDefinition();

  /**
   * True if this program question definition has address correction enabled. Otherwise it is not
   * enabled.
   *
   * <p>This field was added in January 2023. Program question definitions created before this field
   * will default to address correction disabled (false).
   */
  @JsonProperty("addressCorrectionEnabled")
  public abstract boolean addressCorrectionEnabled();

  @JsonIgnore
  public long getProgramDefinitionId() {
    return programDefinitionId().get();
  }

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
      @JsonProperty("id") long id,
      @JsonProperty("optional") boolean optional,
      @JsonProperty("addressCorrectionEnabled") boolean addressCorrectionEnabled) {
    return new AutoValue_ProgramQuestionDefinition(
        id, Optional.empty(), optional, Optional.empty(), addressCorrectionEnabled);
  }

  /** Create a required program question definition. */
  public static ProgramQuestionDefinition create(
      QuestionDefinition questionDefinition, Optional<Long> programDefinitionId) {
    return create(
        questionDefinition,
        programDefinitionId,
        /* optional= */ false,
        /* addressCorrectionEnabled= */ false);
  }

  /** Create a program question definition. */
  public static ProgramQuestionDefinition create(
      QuestionDefinition questionDefinition,
      Optional<Long> programDefinitionId,
      boolean optional,
      boolean addressCorrectionEnabled) {
    return new AutoValue_ProgramQuestionDefinition(
        questionDefinition.getId(),
        programDefinitionId,
        optional,
        Optional.of(questionDefinition),
        addressCorrectionEnabled);
  }

  /**
   * Return a program question definition that is completely loaded with a program definition id and
   * {@link QuestionDefinition} set.
   */
  public ProgramQuestionDefinition loadCompletely(
      long programDefinitionId, QuestionDefinition questionDefinition) {
    return new AutoValue_ProgramQuestionDefinition(
        questionDefinition.getId(),
        Optional.of(programDefinitionId),
        optional(),
        Optional.of(questionDefinition),
        addressCorrectionEnabled());
  }

  /** Return a program question definition with a new optional setting. */
  public ProgramQuestionDefinition setOptional(boolean optional) {
    return create(
        getQuestionDefinition(), programDefinitionId(), optional, addressCorrectionEnabled());
  }

  /** Return a program question definition with an address correction enabled setting. */
  public ProgramQuestionDefinition setAddressCorrectionEnabled(boolean addressCorrectionEnabled) {
    return create(
        getQuestionDefinition(), programDefinitionId(), optional(), addressCorrectionEnabled);
  }
}
