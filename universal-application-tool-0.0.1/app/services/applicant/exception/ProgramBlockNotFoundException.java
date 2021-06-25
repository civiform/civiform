package services.applicant.exception;

public class ProgramBlockNotFoundException extends Exception {
  public ProgramBlockNotFoundException(long programId, String blockId) {
    super("Screen not found in Program (ID " + programId + ") for screen ID " + blockId);
  }
}
