package controllers.admin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;

/**
 * A Jackson-serializable class used to migrate programs between instances. This wrapper class makes
 * serialization easier because it keeps all the data that needs to be serialized in one place.
 *
 * <p>The serialization and deserialization of this class is done in {@link
 * services.migration.ProgramMigrationService}.
 */
public final class ProgramMigrationWrapper {
  /** The definition of the program being migrated. */
  private ProgramDefinition program;

  /** The list of questions contained in the {@code program}. TODO more */
  private ImmutableList<QuestionDefinition> questions;

  @JsonCreator
  public ProgramMigrationWrapper(
      @JsonProperty("program") ProgramDefinition program,
      @JsonProperty("questions") ImmutableList<QuestionDefinition> questions) {
    this.program = program;
    this.questions = questions;
  }

  public ProgramDefinition getProgram() {
    return program;
  }

  public void setProgram(ProgramDefinition program) {
    this.program = program;
  }

  public ImmutableList<QuestionDefinition> getQuestions() {
    return questions;
  }

  public void setQuestions(ImmutableList<QuestionDefinition> questions) {
    this.questions = questions;
  }
}
