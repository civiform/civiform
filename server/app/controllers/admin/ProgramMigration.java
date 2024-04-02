package controllers.admin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;

/**
 * A Jackson serializable class used to migrate programs between instances. {@link
 * AdminExportController} creates an instance of this class and serializes it to JSON. {@link
 * AdminImportController} de-serializes the JSON back into this class.
 */
public class ProgramMigration {
  /** The definition of the program being migrated. */
  private ProgramDefinition program;

  /**
   * All the questions that {@code program} contains. TODO explain why they're stored separately.
   */
  private ImmutableList<QuestionDefinition> questions;

  @JsonCreator
  public ProgramMigration(
      @JsonProperty("program") ProgramDefinition program,
      @JsonProperty("questions") ImmutableList<QuestionDefinition> questions) {
    this.program = program;
    this.questions = questions;
  }

  public ProgramDefinition getProgram() {
    return program;
  }

  public void setPrograms(ProgramDefinition programs) {
    this.program = programs;
  }

  public ImmutableList<QuestionDefinition> getQuestions() {
    return questions;
  }

  public void setQuestions(ImmutableList<QuestionDefinition> questions) {
    this.questions = questions;
  }
}
