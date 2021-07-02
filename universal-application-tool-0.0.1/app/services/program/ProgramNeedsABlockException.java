package services.program;

/** ProgramNeedsABlockException is thrown when a porgram has no block. */
public class ProgramNeedsABlockException extends Exception {
  public ProgramNeedsABlockException(long programId) {
    super(String.format("A program needs to have at least a block (program ID %d)", programId));
  }
}
