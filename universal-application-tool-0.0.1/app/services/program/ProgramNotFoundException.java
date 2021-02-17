package services.program;

public class ProgramNotFoundException extends Exception {
  public ProgramNotFoundException(long id) {
    super("Program not found for ID: " + id);
  }
}
