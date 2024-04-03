package controllers.admin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import services.program.ProgramDefinition;

/**
 * A Jackson serializable class used to migrate programs between instances. {@link
 * AdminExportController} creates an instance of this class and serializes it to JSON. {@link
 * AdminImportController} de-serializes the JSON back into this class.
 */
public final class ProgramMigration {
  /** The definition of the program being migrated. */
  private ProgramDefinition program;

  // TODO(#7087): The program definition will only contain the IDs of the questions it has, but not
  // the question definition itself. We should also export the question definitions.

  @JsonCreator
  public ProgramMigration(@JsonProperty("program") ProgramDefinition program) {
    this.program = program;
  }

  public ProgramDefinition getProgram() {
    return program;
  }

  public void setPrograms(ProgramDefinition programs) {
    this.program = programs;
  }
}
