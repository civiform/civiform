package services.program;

public class ProgramNeedsABlockException extends Exception {
  public ProgramNeedsABlockException(long programId) {
    super(String.format("Cannot delete the last block of program (ID %d)", programId));
  }
}
