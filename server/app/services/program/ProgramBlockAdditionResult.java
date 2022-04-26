package services.program;

import com.google.auto.value.AutoValue;
import java.util.Optional;

/**
 * The result of a call to {@link ProgramService.addRepeatedBlockToProgram} or {@link
 * ProgramService.addBlockToProgram}.
 */
@AutoValue
public abstract class ProgramBlockAdditionResult {
  public static ProgramBlockAdditionResult of(
      ProgramDefinition programDefinition, Optional<BlockDefinition> maybeAddedBlockDefinition) {
    return new AutoValue_ProgramBlockAdditionResult(programDefinition, maybeAddedBlockDefinition);
  }

  /** The program containing the block that was added. */
  public abstract ProgramDefinition program();

  /** The newly added block, if no errors were encountered. */
  public abstract Optional<BlockDefinition> maybeAddedBlock();
}
