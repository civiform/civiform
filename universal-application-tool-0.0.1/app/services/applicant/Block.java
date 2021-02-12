package services.applicant;

import com.google.auto.value.AutoValue;

/** Represents a block in the context of a specific user's application. */
@AutoValue
public abstract class Block {

  /**
   * The block's ID. Note this is different from the {@code BlockDefinition}'s ID because BlockDefinitions that repeat
   * expand to multiple Blocks.
   */
  abstract long id();
}
