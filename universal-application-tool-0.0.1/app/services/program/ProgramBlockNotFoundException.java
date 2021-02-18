package services.program;

public class ProgramBlockNotFoundException extends Exception {
  public ProgramBlockNotFoundException(long programid, long blockid) {
    super("Block not found in Program (ID " + programid + ") for block ID " + blockid);
  }
}
