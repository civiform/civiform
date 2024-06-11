package controllers.admin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
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

  /**
   * The list of questions contained in the {@code program}.
   *
   * <p>These questions are stored separately from the {@code program} because:
   *
   * <p>A {@link ProgramDefinition} object contains a list of {@link
   * services.program.BlockDefinition} objects, which specify blocks in a program. Each {@link
   * services.program.BlockDefinition} object includes a list of {@link
   * services.program.ProgramQuestionDefinition} objects, which specify all the questions in a
   * block. These {@link services.program.ProgramQuestionDefinition} objects are stored in the
   * database as a JSON-serialized string that include **only**: 1) The question's ID 2) If it's
   * optional 3) If it has address correction enabled.
   *
   * <p>Because the {@link services.program.ProgramQuestionDefinition}s already specify a
   * serialization structure, we can't change it: If we did, then we couldn't parse definitions
   * already stored in the database. However, this existing serialization structure doesn't have
   * enough information for us to re-create a full question definition.
   *
   * <p>In order to do that re-creation, we fetch the question definitions separately from the
   * program definition. {@link controllers.admin.AdminExportController} collects IDs of all the
   * questions in {@code program}, and then fetches the full {@link QuestionDefinition}s to store in
   * this object for serialization.
   */
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
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
