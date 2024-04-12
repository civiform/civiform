package controllers.admin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import services.program.ProgramDefinition;

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

  // TODO(#7087): The program definition will only contain the IDs of the questions it has, but not
  // the question definitions themselves. We should also export the question definitions.

  @JsonCreator
  public ProgramMigrationWrapper(@JsonProperty("program") ProgramDefinition program) {
    this.program = program;
  }

  public ProgramDefinition getProgram() {
    return program;
  }

  public void setProgram(ProgramDefinition program) {
    this.program = program;
  }
}
